package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.connection.AccessConfiguration;
import com.lasthopesoftware.bluewater.servers.connection.AccessConfigurationBuilder;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.ActiveFileDownloadsActivity;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.receivers.SyncAlarmBroadcastReceiver;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;
import com.lasthopesoftware.threading.IOneParameterRunnable;
import com.lasthopesoftware.threading.ISimpleTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by david on 7/26/15.
 */
public class ItemSyncService extends Service {

	public static final String onFileDownloadedEvent = SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "onFileDownloadedEvent");
	public static final String onFileDownloadedStoreId = SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "onFileDownloadedStoreId");

	private static final String doSyncAction = SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "doSyncAction");
	private static final long syncInterval = 3 * 60 * 60 * 1000; // 3 hours
	private static final int notificationId = 23;

	private static final Logger logger = LoggerFactory.getLogger(ItemSyncService.class);

	private LocalBroadcastManager localBroadcastManager;
	private PowerManager.WakeLock wakeLock;

	private volatile int librariesProcessing;

	private final Runnable finishServiceRunnable = new Runnable() {
		@Override
		public void run() {
			if (--librariesProcessing > 0) return;

			logger.info("Finishing sync. Scheduling next sync for " + syncInterval + "ms from now.");

			// Set an alarm for the next time we run this bad boy
			final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			final PendingIntent pendingIntent = PendingIntent.getBroadcast(ItemSyncService.this, 1, new Intent(SyncAlarmBroadcastReceiver.scheduledSyncIntent), PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + syncInterval, pendingIntent);

			stopForeground(true);
			((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
			stopSelf();
		}
	};

	private final IOneParameterRunnable<StoredFile> storedFileDownloadedAction = new IOneParameterRunnable<StoredFile>() {
		@Override
		public void run(StoredFile storedFile) {
			final Intent fileDownloadedIntent = new Intent(onFileDownloadedEvent);
			fileDownloadedIntent.putExtra(onFileDownloadedStoreId, storedFile.getId());
			localBroadcastManager.sendBroadcast(fileDownloadedIntent);
		}
	};

	public static boolean isSyncScheduled(Context context) {
		return PendingIntent.getBroadcast(context, 0, new Intent(SyncAlarmBroadcastReceiver.scheduledSyncIntent), PendingIntent.FLAG_NO_CREATE) != null;
	}

	public static void doSync(Context context) {
		final Intent intent = new Intent(context, ItemSyncService.class);
		intent.setAction(doSyncAction);

		context.startService(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		final PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "wakeLock"));
		wakeLock.acquire();

		registerReceiver()
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!intent.getAction().equals(doSyncAction)) return START_REDELIVER_INTENT;

		final int result = START_NOT_STICKY;

		final Context context = this;

		if (!IoCommon.isWifiAndPowerConnected(context)) {
			finishServiceRunnable.run();
			return result;
		}

		logger.info("Starting sync.");
		startForeground(notificationId, buildSyncNotification());

		LibrarySession.GetLibraries(context, new ISimpleTask.OnCompleteListener<Void, Void, List<Library>>() {
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Library>> owner, final List<Library> libraries) {
				librariesProcessing += libraries.size();

				if (librariesProcessing == 0) {
					finishServiceRunnable.run();
					return;
				}

				for (final Library library : libraries) {
					AccessConfigurationBuilder.buildConfiguration(context, library, new ISimpleTask.OnCompleteListener<Void, Void, AccessConfiguration>() {
						@Override
						public void onComplete(ISimpleTask<Void, Void, AccessConfiguration> owner, AccessConfiguration accessConfiguration) {
							if (library.isSyncLocalConnectionsOnly())
								accessConfiguration.setLocalOnly(true);
							final ConnectionProvider connectionProvider = new ConnectionProvider(accessConfiguration);

							final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(context, connectionProvider, library);
							librarySyncHandler.setOnFileDownloaded(storedFileDownloadedAction);
							librarySyncHandler.setOnQueueProcessingCompleted(finishServiceRunnable);
							librarySyncHandler.startSync();
						}
					});
				}
			}
		});

		return result;
	}

	private Notification buildSyncNotification() {
		final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this);
		notifyBuilder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		notifyBuilder.setContentTitle(getText(R.string.title_sync_files));
		notifyBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ActiveFileDownloadsActivity.class),0));

		return notifyBuilder.build();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		wakeLock.release();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private final IBinder mBinder = new GenericBinder<>(this);
}
