package com.lasthopesoftware.bluewater

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.UpdatePlayStatsOnCompleteRegistration
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.StorageWritePermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.StorageWritePermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.connection.receivers.SessionConnectionRegistrationsMaintainer
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.PlaybackFileStartedScrobblerRegistration
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.PlaybackFileStoppedScrobblerRegistration
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.exceptions.LoggerUncaughtExceptionHandler
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory
import java.io.File

open class MainApplication : Application() {

	companion object {
		private var isWorkManagerInitialized = false
	}

	private val libraryRepository by lazy { LibraryRepository(this) }
	private val storedFileAccess by lazy { StoredFileAccess(this) }
	private val notificationManagerLazy by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val storageReadPermissionsRequestNotificationBuilderLazy by lazy { StorageReadPermissionsRequestNotificationBuilder(this) }
	private val storageWritePermissionsRequestNotificationBuilderLazy by lazy { StorageWritePermissionsRequestNotificationBuilder(this) }
	private val messageBus by lazy { MessageBus(LocalBroadcastManager.getInstance(this)) }
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
	}

	private fun registerAppBroadcastReceivers() {
		messageBus.registerReceiver(ReceiveBroadcastEvents { intent ->
			val libraryId = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundFileKey, -1)
			if (libraryId < 0) return@ReceiveBroadcastEvents

			val fileKey = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundFileKey, -1)
			if (fileKey == -1) return@ReceiveBroadcastEvents

			val mediaFileId = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundMediaId, -1)
			if (mediaFileId == -1) return@ReceiveBroadcastEvents

			val mediaFilePath = intent.getStringExtra(MediaFileUriProvider.mediaFileFoundPath)
			if (mediaFilePath.isNullOrEmpty()) return@ReceiveBroadcastEvents

			libraryRepository
				.getLibrary(LibraryId(libraryId))
				.then { library ->
					if (library != null) {
						storedFileAccess.addMediaFile(library, ServiceFile(fileKey), mediaFileId, mediaFilePath)
					}
				}
		}, IntentFilter(MediaFileUriProvider.mediaFileFoundEvent))

		messageBus.registerReceiver({ intent ->
			intent.getIntExtra(StorageReadPermissionsRequestedBroadcaster.readPermissionsLibraryId, -1)
				.takeIf { it > -1 }
				?.let { libraryId ->
					notificationManagerLazy.notify(
						336,
						storageReadPermissionsRequestNotificationBuilderLazy
							.buildReadPermissionsRequestNotification(libraryId))
				}
		}, IntentFilter(StorageReadPermissionsRequestedBroadcaster.readPermissionsNeeded))

		messageBus.registerReceiver({ intent ->
			intent.getIntExtra(StorageReadPermissionsRequestedBroadcaster.readPermissionsLibraryId, -1)
				.takeIf { it > -1 }
				?.let { libraryId ->
					notificationManagerLazy.notify(
						396,
						storageWritePermissionsRequestNotificationBuilderLazy
							.buildWritePermissionsRequestNotification(libraryId)
					)
				}
		}, IntentFilter(StorageWritePermissionsRequestedBroadcaster.writePermissionsNeeded))

		messageBus.registerReceiver(
			ConnectionSessionSettingsChangeReceiver(ConnectionSessionManager.get(this)),
			IntentFilter(ObservableConnectionSettingsLibraryStorage.connectionSettingsUpdated)
		)

		messageBus.registerReceiver(
			SelectedConnectionSettingsChangeReceiver(
				SelectedBrowserLibraryIdentifierProvider(applicationSettings),
				messageBus),
			IntentFilter(ObservableConnectionSettingsLibraryStorage.connectionSettingsUpdated)
		)

		val connectionDependentReceiverRegistrations = listOf(
			UpdatePlayStatsOnCompleteRegistration(),
			PlaybackFileStartedScrobblerRegistration(this),
			PlaybackFileStoppedScrobblerRegistration(this))

		messageBus.registerReceiver(
			SessionConnectionRegistrationsMaintainer(this, messageBus, connectionDependentReceiverRegistrations),
			IntentFilter(SelectedConnection.buildSessionBroadcast))

		LiveNowPlayingLookup.initializeInstance(this)
	}

	private fun initializeLogging() {
		val lc = LoggerFactory.getILoggerFactory() as LoggerContext
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

				val rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>()
				rollingPolicy.fileNamePattern = File(logDir, "%d{yyyy-MM-dd}.log").absolutePath
				rollingPolicy.maxHistory = 30
				rollingPolicy.setParent(rollingFileAppender) // parent and context required!
				rollingPolicy.context = lc
				rollingPolicy.start()
				rollingFileAppender.rollingPolicy = rollingPolicy
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
