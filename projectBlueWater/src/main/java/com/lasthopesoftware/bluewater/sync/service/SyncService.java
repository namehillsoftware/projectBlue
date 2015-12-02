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
import com.lasthopesoftware.bluewater.servers.connection.AccessConfiguration;
import com.lasthopesoftware.bluewater.servers.connection.AccessConfigurationBuilder;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.connection.helpers.ConnectionTester;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;
import com.lasthopesoftware.bluewater.sync.receivers.SyncAlarmBroadcastReceiver;
import com.lasthopesoftware.runnables.IOneParameterRunnable;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.Lazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by david on 7/26/15.
 */
public class SyncService extends Service {

	public static final String onFileQueuedEvent = SpecialValueHelpers.buildMagicPropertyName(SyncService.class, "onFileQueuedEvent");
	public static final String onFileDownloadedEvent = SpecialValueHelpers.buildMagicPropertyName(SyncService.class, "onFileDownloadedEvent");
	public static final String storedFileEventKey = SpecialValueHelpers.buildMagicPropertyName(SyncService.class, "storedFileEventKey");

	private static final String doSyncAction = SpecialValueHelpers.buildMagicPropertyName(SyncService.class, "doSyncAction");
	private static final long syncInterval = 3 * 60 * 60 * 1000; // 3 hours
	private static final int notificationId = 23;

	private static final Logger logger = LoggerFactory.getLogger(SyncService.class);

	private LocalBroadcastManager localBroadcastManager;
	private PowerManager.WakeLock wakeLock;

	private volatile int librariesProcessing;
	private HashSet<LibrarySyncHandler> librarySyncHandlers = new HashSet<>();

	private final IOneParameterRunnable<LibrarySyncHandler> onLibrarySyncCompleteRunnable = new IOneParameterRunnable<LibrarySyncHandler>() {
		@Override
		public void run(LibrarySyncHandler librarySyncHandler) {
			librarySyncHandlers.remove(librarySyncHandler);
			if (--librariesProcessing == 0) finishSync();
		}
	};

	private final IOneParameterRunnable<StoredFile> storedFileQueuedAction = new IOneParameterRunnable<StoredFile>() {
		@Override
		public void run(StoredFile storedFile) {
			final Intent fileDownloadedIntent = new Intent(onFileQueuedEvent);
			fileDownloadedIntent.putExtra(storedFileEventKey, storedFile.getId());
			localBroadcastManager.sendBroadcast(fileDownloadedIntent);
		}
	};

	private final IOneParameterRunnable<StoredFile> storedFileDownloadedAction = new IOneParameterRunnable<StoredFile>() {
		@Override
		public void run(StoredFile storedFile) {
			final Intent fileDownloadedIntent = new Intent(onFileDownloadedEvent);
			fileDownloadedIntent.putExtra(storedFileEventKey, storedFile.getId());
			localBroadcastManager.sendBroadcast(fileDownloadedIntent);
		}
	};

	private final Lazy<BroadcastReceiver> onWifiStateChangedReceiver = new Lazy<>(new Callable<BroadcastReceiver>() {
		@Override
		public BroadcastReceiver call() throws Exception {
			return new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (!IoCommon.isWifiConnected(context)) cancelSync();
				}
			};
		}
	});

	private Lazy<BroadcastReceiver> onPowerDisconnectedReceiver = new Lazy<>(new Callable<BroadcastReceiver>() {
		@Override
		public BroadcastReceiver call() throws Exception {
			return new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					cancelSync();
				}
			};
		}
	});

	public static boolean isSyncScheduled(Context context) {
		return PendingIntent.getBroadcast(context, 0, new Intent(SyncAlarmBroadcastReceiver.scheduledSyncIntent), PendingIntent.FLAG_NO_CREATE) != null;
	}

	public static void doSync(Context context) {
		final Intent intent = new Intent(context, SyncService.class);
		intent.setAction(doSyncAction);

		context.startService(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		final PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, SpecialValueHelpers.buildMagicPropertyName(SyncService.class, "wakeLock"));
		wakeLock.acquire();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!intent.getAction().equals(doSyncAction)) return START_REDELIVER_INTENT;

		final int result = START_NOT_STICKY;

		if (!isDeviceStateValidForSync()) {
			finishSync();
			return result;
		}

		final Context context = this;

		logger.info("Starting sync.");
		startForeground(notificationId, buildSyncNotification());

		LibrarySession.GetLibraries(context, new ISimpleTask.OnCompleteListener<Void, Void, List<Library>>() {
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Library>> owner, final List<Library> libraries) {
				librariesProcessing += libraries.size();

				if (librariesProcessing == 0) {
					finishSync();
					return;
				}

				for (final Library library : libraries) {
					AccessConfigurationBuilder.buildConfiguration(context, library, new ISimpleTask.OnCompleteListener<Void, Void, AccessConfiguration>() {
						@Override
						public void onComplete(ISimpleTask<Void, Void, AccessConfiguration> owner, AccessConfiguration accessConfiguration) {
							if (library.isSyncLocalConnectionsOnly())
								accessConfiguration.setLocalOnly(true);

							final ConnectionProvider connectionProvider = new ConnectionProvider(accessConfiguration);
							ConnectionTester.doTest(connectionProvider, 5000, new ISimpleTask.OnCompleteListener<Integer, Void, Boolean>() {
								@Override
								public void onComplete(ISimpleTask<Integer, Void, Boolean> owner, Boolean success) {
									if (!success) {
										if (--librariesProcessing == 0) finishSync();
										return;
									}

									final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(context, connectionProvider, library);
									librarySyncHandler.setOnFileQueued(storedFileQueuedAction);
									librarySyncHandler.setOnFileDownloaded(storedFileDownloadedAction);
									librarySyncHandler.setOnQueueProcessingCompleted(onLibrarySyncCompleteRunnable);
									librarySyncHandler.startSync();

									librarySyncHandlers.add(librarySyncHandler);
								}
							});
						}
					});
				}
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

	private Notification buildSyncNotification() {
		final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this);
		notifyBuilder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		notifyBuilder.setContentTitle(getText(R.string.title_sync_files));
//		notifyBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ActiveFileDownloadsFragment.class), 0));

		return notifyBuilder.build();
	}

	private void cancelSync() {
		for (LibrarySyncHandler librarySyncHandler : librarySyncHandlers) librarySyncHandler.cancel();
	}

	private void finishSync() {
		logger.info("Finishing sync. Scheduling next sync for " + syncInterval + "ms from now.");

		// Set an alarm for the next time we run this bad boy
		final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(SyncService.this, 1, new Intent(SyncAlarmBroadcastReceiver.scheduledSyncIntent), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + syncInterval, pendingIntent);

		stopForeground(true);
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
		stopSelf();
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
