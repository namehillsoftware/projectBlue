package com.lasthopesoftware.bluewater.sync.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.AccessConfigurationBuilder;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.sync.receivers.SyncAlarmBroadcastReceiver;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.lazyj.Lazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

/**
 * Created by david on 7/26/15.
 */
public class SyncService extends Service {

	public static final String onSyncStartEvent = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "onSyncStartEvent");
	public static final String onSyncStopEvent = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "onSyncStopEvent");
	public static final String onFileQueuedEvent = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "onFileQueuedEvent");
	public static final String onFileDownloadingEvent = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "onFileDownloadingEvent");
	public static final String onFileDownloadedEvent = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "onFileDownloadedEvent");
	public static final String storedFileEventKey = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "storedFileEventKey");

	private static final String doSyncAction = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "doSyncAction");
	private static final String cancelSyncAction = MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "cancelSyncAction");
	private static final long syncInterval = 3 * 60 * 60 * 1000; // 3 hours
	private static final int notificationId = 23;

	private static final Logger logger = LoggerFactory.getLogger(SyncService.class);

	private static volatile boolean isSyncRunning;

	private LocalBroadcastManager localBroadcastManager;
	private NotificationManager notificationMgr;
	private PowerManager.WakeLock wakeLock;

	private volatile int librariesProcessing;

	private final HashSet<LibrarySyncHandler> librarySyncHandlers = new HashSet<>();

	private final OneParameterRunnable<LibrarySyncHandler> onLibrarySyncCompleteRunnable = librarySyncHandler -> {
		librarySyncHandlers.remove(librarySyncHandler);

		if (--librariesProcessing == 0) finishSync();
	};

	private final OneParameterRunnable<StoredFile> storedFileQueuedAction = storedFile -> sendStoredFileBroadcast(onFileQueuedEvent, storedFile);

	private final Lazy<String> downloadingStatusLabel = new Lazy<String>() {
		@Override
		protected String initialize() {
			return SyncService.this.getString(R.string.downloading_status_label);
		}
	};

	private final OneParameterRunnable<StoredFile> storedFileDownloadingAction = storedFile -> {
		sendStoredFileBroadcast(onFileDownloadingEvent, storedFile);

		LibrarySession.GetLibrary(SyncService.this, storedFile.getLibraryId(),
			library ->  AccessConfigurationBuilder.buildConfiguration(SyncService.this, library, (parameterOne1, urlProvider) -> {
				if (urlProvider == null) return;

				final ConnectionProvider connectionProvider = new ConnectionProvider(urlProvider);
				final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, storedFile.getServiceId());

				filePropertiesProvider
						.onComplete(fileProperties -> setSyncNotificationText(String.format(downloadingStatusLabel.getObject(), fileProperties.get(FilePropertiesProvider.NAME))))
						.execute();
			}));
	};

	private final OneParameterRunnable<StoredFile> storedFileDownloadedAction = storedFile -> sendStoredFileBroadcast(onFileDownloadedEvent, storedFile);

	private final Lazy<BroadcastReceiver> onWifiStateChangedReceiver = new Lazy<BroadcastReceiver>() {
		@Override
		protected BroadcastReceiver initialize() {
			return new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (!IoCommon.isWifiConnected(context)) cancelSync();
				}
			};
		}
	};

	private final Lazy<BroadcastReceiver> onPowerDisconnectedReceiver = new Lazy<BroadcastReceiver>() {
		@Override
		public BroadcastReceiver initialize() {
			return new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					cancelSync();
				}
			};
		}
	};

	private final Lazy<Intent> browseLibraryIntent = new Lazy<Intent>() {
		@Override
		protected Intent initialize() {
			final Intent browseLibraryIntent = new Intent(SyncService.this, BrowseLibraryActivity.class);
			browseLibraryIntent.setAction(BrowseLibraryActivity.showDownloadsAction);
			browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			return browseLibraryIntent;
		}
	};

	public static boolean isSyncScheduled(Context context) {
		return PendingIntent.getBroadcast(context, 0, new Intent(SyncAlarmBroadcastReceiver.scheduledSyncIntent), PendingIntent.FLAG_NO_CREATE) != null;
	}

	public static boolean isSyncRunning() {
		return isSyncRunning;
	}

	public static void doSync(Context context) {
		final Intent intent = new Intent(context, SyncService.class);
		intent.setAction(doSyncAction);

		context.startService(intent);
	}

	public static void cancelSync(Context context) {
		final Intent intent = new Intent(context, SyncService.class);
		intent.setAction(cancelSyncAction);

		context.startService(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		final PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MagicPropertyBuilder.buildMagicPropertyName(SyncService.class, "wakeLock"));
		wakeLock.acquire();
		notificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent.getAction();

		if (cancelSyncAction.equals(action)) cancelSync();

		if (!doSyncAction.equals(action)) return START_REDELIVER_INTENT;

		final int result = START_NOT_STICKY;

		if (!isDeviceStateValidForSync()) {
			finishSync();
			return result;
		}

		final Context context = this;

		logger.info("Starting sync.");

		isSyncRunning = true;
		startForeground(notificationId, buildSyncNotification(null));
		localBroadcastManager.sendBroadcast(new Intent(onSyncStartEvent));

		LibrarySession.GetLibraries(context, libraries -> {
			librariesProcessing += libraries.size();

			if (librariesProcessing == 0) {
				finishSync();
				return;
			}

			for (final Library library : libraries) {
				if (library.isSyncLocalConnectionsOnly())
					library.setLocalOnly(true);

				AccessConfigurationBuilder.buildConfiguration(context, library, (owner1, urlProvider) -> {
					if (urlProvider == null) {
						if (--librariesProcessing == 0) finishSync();
						return;
					}

					final ConnectionProvider connectionProvider = new ConnectionProvider(urlProvider);

					final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(context, connectionProvider, library);
					librarySyncHandler.setOnFileQueued(storedFileQueuedAction);
					librarySyncHandler.setOnFileDownloading(storedFileDownloadingAction);
					librarySyncHandler.setOnFileDownloaded(storedFileDownloadedAction);
					librarySyncHandler.setOnQueueProcessingCompleted(onLibrarySyncCompleteRunnable);
					librarySyncHandler.startSync();

					librarySyncHandlers.add(librarySyncHandler);
				});
			}
		});

		return result;
	}

	private boolean isDeviceStateValidForSync() {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		final boolean isSyncOnWifiOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, false);
		if (isSyncOnWifiOnly) {
			if (!IoCommon.isWifiConnected(this)) return false;

			registerReceiver(onWifiStateChangedReceiver.getObject(), new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		}

		final boolean isSyncOnPowerOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false);
		if (isSyncOnPowerOnly) {
			if (!IoCommon.isPowerConnected(this)) return false;

			registerReceiver(onPowerDisconnectedReceiver.getObject(), new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
		}

		return true;
	}

	private Notification buildSyncNotification(String syncNotification) {
		final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this);
		notifyBuilder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		notifyBuilder.setContentTitle(getText(R.string.title_sync_files));
		if (syncNotification != null)
			notifyBuilder.setContentText(syncNotification);
		notifyBuilder.setContentIntent(PendingIntent.getActivity(this, 0, browseLibraryIntent.getObject(), 0));

		notifyBuilder.setOngoing(true);

		return notifyBuilder.build();
	}

	private void setSyncNotificationText(String syncNotification) {
		notificationMgr.notify(notificationId, buildSyncNotification(syncNotification));
	}

	private void sendStoredFileBroadcast(String action, StoredFile storedFile) {
		final Intent storedFileBroadcastIntent = new Intent(action);
		storedFileBroadcastIntent.putExtra(storedFileEventKey, storedFile.getId());
		localBroadcastManager.sendBroadcast(storedFileBroadcastIntent);
	}

	private void cancelSync() {
		for (LibrarySyncHandler librarySyncHandler : librarySyncHandlers) librarySyncHandler.cancel();
	}

	private void finishSync() {
		logger.info("Finishing sync. Scheduling next sync for " + syncInterval + "ms from now.");

		// Set an alarm for the next time we run this bad boy
		final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, new Intent(SyncAlarmBroadcastReceiver.scheduledSyncIntent), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + syncInterval, pendingIntent);

		stopForeground(true);
		notificationMgr.cancel(notificationId);
		stopSelf();

		isSyncRunning = false;
		localBroadcastManager.sendBroadcast(new Intent(onSyncStopEvent));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (onWifiStateChangedReceiver.isInitialized())
			unregisterReceiver(onWifiStateChangedReceiver.getObject());

		if (onPowerDisconnectedReceiver.isInitialized())
			unregisterReceiver(onPowerDisconnectedReceiver.getObject());

		wakeLock.release();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private final IBinder mBinder = new GenericBinder<>(this);
}
