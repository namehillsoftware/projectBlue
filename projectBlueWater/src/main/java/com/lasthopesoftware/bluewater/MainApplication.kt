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
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import ch.qos.logback.core.util.StatusPrinter
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdatePlayStatsOnPlaybackCompleteReceiver
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.LibraryPlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.StorageWritePermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.StorageWritePermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.receivers.SessionConnectionRegistrationsMaintainer
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.PlaybackFileStartedScrobblerRegistration
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.PlaybackFileStoppedScrobblerRegistration
import com.lasthopesoftware.bluewater.client.servers.version.LibraryServerVersionProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.SyncItemStateChangedListener
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.exceptions.LoggerUncaughtExceptionHandler
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory
import java.io.File

open class MainApplication : Application() {

	companion object {
		private var isWorkManagerInitialized = false
	}

	private val libraryConnections by lazy { ConnectionSessionManager.get(this) }
	private val libraryRevisionProvider by lazy { LibraryRevisionProvider(libraryConnections) }
	private val libraryRepository by lazy { LibraryRepository(this) }
	private val storedFileAccess by lazy { StoredFileAccess(this) }
	private val notificationManagerLazy by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val storageReadPermissionsRequestNotificationBuilder by lazy { StorageReadPermissionsRequestNotificationBuilder(this) }
	private val storageWritePermissionsRequestNotificationBuilder by lazy { StorageWritePermissionsRequestNotificationBuilder(this) }
	private val applicationMessageBus by lazy { getApplicationMessageBus() }
	private val applicationSettings by lazy { getApplicationSettingsRepository() }

	@SuppressLint("DefaultLocale")
	override fun onCreate() {
		super.onCreate()

		initializeLogging()

		Thread.setDefaultUncaughtExceptionHandler(LoggerUncaughtExceptionHandler)
		Promise.Rejections.setUnhandledRejectionsReceiver(LoggerUncaughtExceptionHandler)

		registerAppBroadcastReceivers()

		if (!isWorkManagerInitialized) {
			WorkManager.initialize(this, Configuration.Builder().build())
			isWorkManagerInitialized = true
		}

		SyncScheduler
			.promiseIsScheduled(this)
			.then { isScheduled -> if (!isScheduled) SyncScheduler.scheduleSync(this) }

		LiveNowPlayingLookup.initializeInstance(this)
	}

	private fun registerAppBroadcastReceivers() {
		applicationMessageBus.registerReceiver { mediaFileFound : MediaFileUriProvider.MediaFileFound ->
			mediaFileFound.mediaId ?: return@registerReceiver
			libraryRepository
				.getLibrary(mediaFileFound.libraryId)
				.then { library ->
					if (library != null) {
						storedFileAccess.addMediaFile(
							library,
							mediaFileFound.serviceFile,
							mediaFileFound.mediaId,
							mediaFileFound.systemFile.path)
					}
				}
		}

		applicationMessageBus.registerReceiver { readPermissionsNeeded : StorageReadPermissionsRequestedBroadcaster.ReadPermissionsNeeded ->
			notificationManagerLazy.notify(
				336,
				storageReadPermissionsRequestNotificationBuilder
					.buildReadPermissionsRequestNotification(readPermissionsNeeded.libraryId.id))
		}

		applicationMessageBus.registerReceiver { writePermissionsNeeded : StorageWritePermissionsRequestedBroadcaster.WritePermissionsNeeded ->
			notificationManagerLazy.notify(
				396,
				storageWritePermissionsRequestNotificationBuilder
					.buildWritePermissionsRequestNotification(writePermissionsNeeded.libraryId.id)
			)
		}

		applicationMessageBus.registerReceiver(ConnectionSessionSettingsChangeReceiver(libraryConnections))
		applicationMessageBus.registerReceiver(
			SelectedConnectionSettingsChangeReceiver(
				SelectedLibraryIdProvider(applicationSettings),
				applicationMessageBus
			)
		)

		val connectionDependentReceiverRegistrations = listOf(
			PlaybackFileStartedScrobblerRegistration(this),
			PlaybackFileStoppedScrobblerRegistration(this))

		applicationMessageBus.registerReceiver(
			UpdatePlayStatsOnPlaybackCompleteReceiver(
				LibraryPlaystatsUpdateSelector(
					LibraryServerVersionProvider(libraryConnections),
					PlayedFilePlayStatsUpdater(
						libraryConnections
					),
					FilePropertiesPlayStatsUpdater(
						FilePropertiesProvider(
							libraryConnections,
							libraryRevisionProvider,
							FilePropertyCache,
						),
						FilePropertyStorage(
							libraryConnections,
							ConnectionAuthenticationChecker(libraryConnections),
							libraryRevisionProvider,
							FilePropertyCache,
							applicationMessageBus
						),
					),
				)
			)
		)

		applicationMessageBus.registerReceiver(SessionConnectionRegistrationsMaintainer(
			this,
			applicationMessageBus,
			connectionDependentReceiverRegistrations
		))

		applicationMessageBus.registerReceiver(SyncItemStateChangedListener(this))
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
		if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() && getExternalFilesDir(null) != null) {
			val asyncAppender = AsyncAppender()
			asyncAppender.context = lc
			asyncAppender.name = "ASYNC"
			val externalFilesDir = getExternalFilesDir(null)
			if (externalFilesDir != null) {
				val logDir = File(externalFilesDir.path + File.separator + "logs")
				if (!logDir.exists()) logDir.mkdirs()
				val filePle = PatternLayoutEncoder()
				filePle.pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
				filePle.context = lc
				filePle.start()

				val rollingFileAppender = RollingFileAppender<ILoggingEvent>()
				rollingFileAppender.lazy = true
				rollingFileAppender.isAppend = true
				rollingFileAppender.context = lc
				rollingFileAppender.encoder = filePle

				rollingFileAppender.rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().apply {
					fileNamePattern = File(logDir, "%d{yyyy-MM-dd}.log").absolutePath
					maxHistory = 30
					setParent(rollingFileAppender) // parent and context required!
					context = lc
					start()
				}

				rollingFileAppender.triggeringPolicy = SizeBasedTriggeringPolicy<ILoggingEvent>().apply {
					maxFileSize = FileSize.valueOf("512 mb")
					context = lc
					start()
				}

				rollingFileAppender.start()

				asyncAppender.addAppender(rollingFileAppender)
			}

			// UNCOMMENT TO TWEAK OPTIONAL SETTINGS
		    // excluding caller data (used for stack traces) improves appender's performance
			asyncAppender.isIncludeCallerData = !DebugFlag.isDebugCompilation

			asyncAppender.start()
			rootLogger.addAppender(asyncAppender)
			StatusPrinter.print(lc)
		}

		val logger = LoggerFactory.getLogger(javaClass)
		logger.info("Uncaught exceptions logging to custom uncaught exception handler.")

		if (!DebugFlag.isDebugCompilation) return

		rootLogger.level = Level.DEBUG
		logger.info("DEBUG_MODE active")
		StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
			.detectDiskReads()
			.detectDiskWrites()
			.detectNetwork() // or .detectAll() for all detectable problems
			.penaltyLog()
			.build())
		StrictMode.setVmPolicy(VmPolicy.Builder()
			.detectLeakedSqlLiteObjects()
			.penaltyLog()
			.build())
	}
}
