package com.lasthopesoftware.bluewater;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.servers.settings.EditServerSettingsActivity;
import com.lasthopesoftware.bluewater.shared.exceptions.LoggerUncaughtExceptionHandler;
import com.lasthopesoftware.bluewater.sync.service.SyncService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.StatusPrinter;

public class MainApplication extends Application {
	
	private static final boolean DEBUG_MODE = com.lasthopesoftware.bluewater.BuildConfig.DEBUG;
	
	@SuppressLint("DefaultLocale")
	@Override
	public void onCreate() {
		super.onCreate();

		initializeLogging();
		Thread.setDefaultUncaughtExceptionHandler(new LoggerUncaughtExceptionHandler());
		registerAppBroadcastReceivers(LocalBroadcastManager.getInstance(this));

		// Kick off a file sync if one isn't scheduled on start-up
		if (!SyncService.isSyncScheduled(this)) SyncService.doSync(this);

		checkPermissions();
	}

	private void checkPermissions() {
		LibrarySession.GetActiveLibrary(this, library -> {
			if (library == null) return;

			if (
				(library.isExternalReadAccessNeeded() && ContextCompat.checkSelfPermission(MainApplication.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) ||
				(library.isExternalWriteAccessNeeded()  && ContextCompat.checkSelfPermission(MainApplication.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED)) {
				showPermissionsAlertDialog();

				final Intent settingsIntent = new Intent(MainApplication.this, EditServerSettingsActivity.class);
				settingsIntent.putExtra(EditServerSettingsActivity.serverIdExtra, library.getId());

				startActivity(settingsIntent);
			}
		});
	}

	private void showPermissionsAlertDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setCancelable(false);
		builder
			.setTitle(getString(R.string.permissions_needed))
			.setMessage(getString(R.string.permissions_needed_launch_settings));

		builder.show();
	}
	
	private void registerAppBroadcastReceivers(LocalBroadcastManager localBroadcastManager) {

		localBroadcastManager.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				LibrarySession.GetActiveLibrary(context, library -> {
					if (library == null) return;

					final StoredFileAccess storedFileAccess = new StoredFileAccess(context, library);
					final int fileKey = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundFileKey, -1);
					if (fileKey == -1) return;

					final int mediaFileId = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundMediaId, -1);
					if (mediaFileId == -1) return;

					final String mediaFilePath = intent.getStringExtra(MediaFileUriProvider.mediaFileFoundPath);
					if (mediaFilePath == null || mediaFilePath.isEmpty()) return;

					storedFileAccess.addMediaFile(new com.lasthopesoftware.bluewater.servers.library.items.media.files.File(fileKey), mediaFileId, mediaFilePath);
				});
			}
		}, new IntentFilter(MediaFileUriProvider.mediaFileFoundEvent));
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

		if (!DEBUG_MODE) return;

		rootLogger.setLevel(Level.INFO);

		logger.info("DEBUG_MODE active");
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads()
				.detectDiskWrites()
				.detectNetwork()   // or .detectAll() for all detectable problems
				.penaltyLog()
				.build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects()
				.detectLeakedClosableObjects()
				.penaltyLog()
						//	                 .penaltyDeath()
				.build());
	}
}
