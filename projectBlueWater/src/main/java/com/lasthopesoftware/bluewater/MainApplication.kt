package com.lasthopesoftware.bluewater

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.work.Configuration
import androidx.work.WorkManager
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.StatusPrinter
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.PlaybackFileStartedScrobbleDroidProxy
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.PlaybackFileStoppedScrobbleDroidProxy
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.ScrobbleIntentProvider
import com.lasthopesoftware.bluewater.client.stored.library.permissions.StoragePermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.permissions.read.StorageReadPermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.permissions.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.SyncChannelProperties
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.SyncItemStateChangedListener
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsUpdated
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.exceptions.LoggerUncaughtExceptionHandler
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.compilation.DebugFlag
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory
import java.io.File

open class MainApplication : Application(), ApplicationDependencies {

	companion object {
		private val logger by lazyLogger<MainApplication>()
		private var isWorkManagerInitialized = false

		val Context.applicationDependencies: ApplicationDependencies
			get() = applicationContext as ApplicationDependencies
	}

	private val libraryConnections by lazy { ConnectionSessionManager.get(this) }

	private val libraryRevisionProvider by lazy { LibraryRevisionProvider(libraryConnections) }

	private val freshLibraryFileProperties by lazy {
		FilePropertiesProvider(
			GuaranteedLibraryConnectionProvider(libraryConnections),
			libraryRevisionProvider,
			FilePropertyCache,
		)
	}

	private val cachedLibraryFileProperties by lazy {
		CachedFilePropertiesProvider(
			libraryConnections,
			FilePropertyCache,
			freshLibraryFileProperties,
		)
	}

	private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

	private val syncChannelProperties by lazy { SyncChannelProperties(this) }

	private val storageReadPermissionsConfiguration by lazy {
		NotificationsConfiguration(
			syncChannelProperties.channelId,
			336
		)
	}

	private val storagePermissionsRequestNotificationBuilder by lazy {
		StoragePermissionsRequestNotificationBuilder(
			this,
			StringResources(this),
			intentBuilder,
			syncChannelProperties
		)
	}

	private val storageReadPermissionsRequestNotificationBuilder by lazy {
		StorageReadPermissionsRequestNotificationBuilder(storagePermissionsRequestNotificationBuilder)
	}

	private val applicationMessageBus by lazy { getApplicationMessageBus() }

	private val applicationSettings by lazy { getApplicationSettingsRepository() }

	@Volatile
	private var isLoggingToFile = true

	override val intentBuilder
		get() = IntentBuilder(this)

	override val syncScheduler
		get() = SyncScheduler(this)

	@SuppressLint("DefaultLocale")
	override fun onCreate() {
		super.onCreate()

		Promise.Rejections.toggleStackTraceFiltering(true)

		initializeLogging()

		Thread.setDefaultUncaughtExceptionHandler(LoggerUncaughtExceptionHandler)
		Promise.Rejections.setUnhandledRejectionsReceiver(LoggerUncaughtExceptionHandler)

		applicationSettings
			.promiseApplicationSettings()
			.then(::reinitializeLoggingIfNecessary)

		registerAppBroadcastReceivers()

		if (!isWorkManagerInitialized) {
			WorkManager.initialize(this, Configuration.Builder().build())
			isWorkManagerInitialized = true
		}

		syncScheduler
			.promiseIsScheduled()
			.then { isScheduled -> if (!isScheduled) syncScheduler.scheduleSync() }

		LiveNowPlayingLookup.initializeInstance(this)
	}

	private fun registerAppBroadcastReceivers() {
		applicationMessageBus.registerReceiver { readPermissionsNeeded : StorageReadPermissionsRequestedBroadcaster.ReadPermissionsNeeded ->
			notificationManager.notify(
				storageReadPermissionsConfiguration.notificationId,
				storageReadPermissionsRequestNotificationBuilder
					.buildReadPermissionsRequestNotification(readPermissionsNeeded.libraryId.id))
		}

		applicationMessageBus.registerReceiver(ConnectionSessionSettingsChangeReceiver(libraryConnections))

		applicationMessageBus.registerReceiver(
			PlaybackFileStartedScrobbleDroidProxy(
			this,
				cachedLibraryFileProperties,
				ScrobbleIntentProvider,
			)
		)

		applicationMessageBus.registerReceiver(
			PlaybackFileStoppedScrobbleDroidProxy(this, ScrobbleIntentProvider)
		)

		applicationMessageBus.registerReceiver(SyncItemStateChangedListener(syncScheduler))

		applicationMessageBus.registerReceiver { _: ApplicationSettingsUpdated ->
			applicationSettings
				.promiseApplicationSettings()
				.then(::reinitializeLoggingIfNecessary)
		}
	}

	private fun reinitializeLoggingIfNecessary(applicationSettings: ApplicationSettings) {
		if (applicationSettings.isLoggingToFile != isLoggingToFile) {
			isLoggingToFile = applicationSettings.isLoggingToFile
			initializeLogging()

			logger.info("File logging {}.", if (isLoggingToFile) "enabled" else "disabled")
		}
	}

	private fun initializeLogging() {
		val loggerFactory = LoggerFactory.getILoggerFactory()
		val lc = loggerFactory as? LoggerContext ?: return
		lc.reset()

		// setup LogcatAppender
		val logcatEncoder = PatternLayoutEncoder()
		logcatEncoder.context = lc
		logcatEncoder.pattern = "[%thread] %msg%n"
		logcatEncoder.start()
		val logcatAppender = LogcatAppender()
		logcatAppender.context = lc
		logcatAppender.encoder = logcatEncoder
		logcatAppender.start()
		// add the newly created appenders to the root logger;
		// qualify Logger to disambiguate from org.slf4j.Logger
		val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN
		rootLogger.addAppender(logcatAppender)
		if (isLoggingToFile && Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() && getExternalFilesDir(null) != null) {
			val asyncAppender = AsyncAppender()
			asyncAppender.context = lc
			asyncAppender.name = "ASYNC"
			val externalFilesDir = getExternalFilesDir(null)
			if (externalFilesDir != null) {
				val logDir = File(externalFilesDir.path + File.separator + "logs")
				if (!logDir.exists()) logDir.mkdirs()
				val rollingFileAppender = RollingFileAppender<ILoggingEvent>().apply {
					lazy = true
					isAppend = true
					context = lc
					file = File(logDir, "log.log").absolutePath

					rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>()
						.also { it.setParent(this) }
						.apply {
							fileNamePattern = File(logDir, "%d{yyyy-MM-dd}.log").absolutePath
							maxHistory = 5
							context = lc
							start()
						}

					encoder = PatternLayoutEncoder().apply {
						pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
						context = lc
						start()
					}

					start()
				}

				asyncAppender.addAppender(rollingFileAppender)
			}

			// UNCOMMENT TO TWEAK OPTIONAL SETTINGS
		    // excluding caller data (used for stack traces) improves appender's performance
			asyncAppender.isIncludeCallerData = !DebugFlag.isDebugCompilation

			asyncAppender.start()
			rootLogger.addAppender(asyncAppender)
			StatusPrinter.print(lc)
		}

		if (DebugFlag.isDebugCompilation) {
			rootLogger.level = Level.DEBUG
			logger.info("DEBUG_MODE active")
			StrictMode.setThreadPolicy(
				StrictMode.ThreadPolicy.Builder()
					.detectDiskReads()
					.detectDiskWrites()
					.detectNetwork() // or .detectAll() for all detectable problems
					.penaltyLog()
					.build()
			)
			StrictMode.setVmPolicy(
				VmPolicy.Builder()
					.detectLeakedSqlLiteObjects()
					.penaltyLog()
					.build()
			)
		}
	}
}
