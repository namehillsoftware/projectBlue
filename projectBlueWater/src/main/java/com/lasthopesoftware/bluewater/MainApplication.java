package com.lasthopesoftware.bluewater;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.StatusPrinter;
import com.lasthopesoftware.bluewater.client.connection.AccessConfigurationBuilder;
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration;
import com.lasthopesoftware.bluewater.client.connection.receivers.SessionConnectionRegistrationsMaintainer;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.UpdatePlayStatsOnCompleteRegistration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.retrieval.StoredFilesCollection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.IStorageReadPermissionsRequestNotificationBuilder;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.StorageReadPermissionsRequestNotificationBuilder;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.StorageReadPermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.IStorageWritePermissionsRequestNotificationBuilder;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.StorageWritePermissionsRequestNotificationBuilder;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.StorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.pebble.PebbleFileChangedNotificationRegistration;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.PlaybackFileStartedScrobblerRegistration;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble.PlaybackFileStoppedScrobblerRegistration;
import com.lasthopesoftware.bluewater.shared.exceptions.LoggerUncaughtExceptionHandler;
import com.lasthopesoftware.bluewater.sync.SyncWorker;
import com.lasthopesoftware.compilation.DebugFlag;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

public class MainApplication extends Application {
	
	private final Lazy<NotificationManager> notificationManagerLazy = new Lazy<>(() -> (NotificationManager) getSystemService(NOTIFICATION_SERVICE));
	private final Lazy<IStorageReadPermissionsRequestNotificationBuilder> storageReadPermissionsRequestNotificationBuilderLazy = new Lazy<>(() -> new StorageReadPermissionsRequestNotificationBuilder(this));
	private final Lazy<IStorageWritePermissionsRequestNotificationBuilder> storageWritePermissionsRequestNotificationBuilderLazy = new Lazy<>(() -> new StorageWritePermissionsRequestNotificationBuilder(this));
	
	@SuppressLint("DefaultLocale")
	@Override
	public void onCreate() {
		super.onCreate();

		initializeLogging();
		Thread.setDefaultUncaughtExceptionHandler(new LoggerUncaughtExceptionHandler());
		registerAppBroadcastReceivers(LocalBroadcastManager.getInstance(this));

		WorkManager.initialize(this, new Configuration.Builder().build());
		SyncWorker.promiseIsScheduled()
			.then(isScheduled -> isScheduled
				? SyncWorker.scheduleSync()
				: null);
	}

	private void registerAppBroadcastReceivers(LocalBroadcastManager localBroadcastManager) {

		localBroadcastManager.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				final int libraryId = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundFileKey, -1);
				if (libraryId < 0) return;

				new LibraryRepository(context)
					.getLibrary(libraryId)
					.eventually(library ->
						AccessConfigurationBuilder.buildConfiguration(context, library).then(new VoidResponse<>(urlProvider -> {
							if (urlProvider == null) return;

							final int fileKey = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundFileKey, -1);
							if (fileKey == -1) return;

							final int mediaFileId = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundMediaId, -1);
							if (mediaFileId == -1) return;

							final String mediaFilePath = intent.getStringExtra(MediaFileUriProvider.mediaFileFoundPath);
							if (mediaFilePath == null || mediaFilePath.isEmpty()) return;

							final StoredFileAccess storedFileAccess = new StoredFileAccess(
								context,
								new StoredFilesCollection(context));

							storedFileAccess.addMediaFile(library, new ServiceFile(fileKey), mediaFileId, mediaFilePath);
					})));
			}
		}, new IntentFilter(MediaFileUriProvider.mediaFileFoundEvent));

		localBroadcastManager.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final int libraryId = intent.getIntExtra(StorageReadPermissionsRequestedBroadcaster.ReadPermissionsLibraryId, -1);
				if (libraryId < 0) return;

				notificationManagerLazy.getObject().notify(
						336,
						storageReadPermissionsRequestNotificationBuilderLazy
								.getObject()
								.buildReadPermissionsRequestNotification(libraryId));
			}
		}, new IntentFilter(StorageReadPermissionsRequestedBroadcaster.ReadPermissionsNeeded));

		localBroadcastManager.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final int libraryId = intent.getIntExtra(StorageWritePermissionsRequestedBroadcaster.WritePermissionsLibraryId, -1);
				if (libraryId < 0) return;

				notificationManagerLazy.getObject().notify(
						396,
						storageWritePermissionsRequestNotificationBuilderLazy
								.getObject()
								.buildWritePermissionsRequestNotification(libraryId));
			}
		}, new IntentFilter(StorageWritePermissionsRequestedBroadcaster.WritePermissionsNeeded));

		final Collection<IConnectionDependentReceiverRegistration> connectionDependentReceiverRegistrations =
			Arrays.asList(
				new UpdatePlayStatsOnCompleteRegistration(),
				new PlaybackFileStartedScrobblerRegistration(),
				new PlaybackFileStoppedScrobblerRegistration(),
				new PebbleFileChangedNotificationRegistration());

		localBroadcastManager.registerReceiver(
			new SessionConnectionRegistrationsMaintainer(localBroadcastManager, connectionDependentReceiverRegistrations),
			new IntentFilter(SessionConnection.buildSessionBroadcast));
	}

	private void initializeLogging() {
		final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.reset();

		// setup LogcatAppender
		final PatternLayoutEncoder logcatEncoder = new PatternLayoutEncoder();
		logcatEncoder.setContext(lc);
		logcatEncoder.setPattern("[%thread] %msg%n");
		logcatEncoder.start();

		final LogcatAppender logcatAppender = new LogcatAppender();
		logcatAppender.setContext(lc);
		logcatAppender.setEncoder(logcatEncoder);
		logcatAppender.start();
		// add the newly created appenders to the root logger;
		// qualify Logger to disambiguate from org.slf4j.Logger
		final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.setLevel(Level.WARN);

		rootLogger.addAppender(logcatAppender);

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && getExternalFilesDir(null) != null) {
			final AsyncAppender asyncAppender = new AsyncAppender();
			asyncAppender.setContext(lc);
			asyncAppender.setName("ASYNC");

			final File externalFilesDir = getExternalFilesDir(null);
			if (externalFilesDir != null) {
				final File logDir = new File(externalFilesDir.getPath() + File.separator + "logs");
				if (!logDir.exists())
					logDir.mkdirs();

				final PatternLayoutEncoder filePle = new PatternLayoutEncoder();
				filePle.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
				filePle.setContext(lc);
				filePle.start();

				final RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
				rollingFileAppender.setLazy(true);
				rollingFileAppender.setAppend(true);
				rollingFileAppender.setContext(lc);
				rollingFileAppender.setEncoder(filePle);

				final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
				rollingPolicy.setFileNamePattern(new File(logDir, "%d{yyyy-MM-dd}.log").getAbsolutePath());
				rollingPolicy.setMaxHistory(30);
				rollingPolicy.setParent(rollingFileAppender);  // parent and context required!
				rollingPolicy.setContext(lc);
				rollingPolicy.start();

				rollingFileAppender.setRollingPolicy(rollingPolicy);
				rollingFileAppender.start();

				asyncAppender.addAppender(rollingFileAppender);
			}

			// UNCOMMENT TO TWEAK OPTIONAL SETTINGS
//		    // excluding caller data (used for stack traces) improves appender's performance
//		    asyncAppender.setIncludeCallerData(false);
//		    // set threshold to 0 to disable discarding and keep all events
//		    asyncAppender.setDiscardingThreshold(0);
//		    asyncAppender.setQueueSize(256);

			asyncAppender.start();

			rootLogger.addAppender(asyncAppender);
			StatusPrinter.print(lc);
		}

		final Logger logger = LoggerFactory.getLogger(getClass());
		logger.info("Uncaught exceptions logging to custom uncaught exception handler.");

		if (!DebugFlag.getInstance().isDebugCompilation()) return;

		rootLogger.setLevel(Level.DEBUG);

		logger.info("DEBUG_MODE active");
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads()
				.detectDiskWrites()
				.detectNetwork()   // or .detectAll() for all detectable problems
				.penaltyLog()
				.build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects()
				.penaltyLog()
				.build());
	}
}
