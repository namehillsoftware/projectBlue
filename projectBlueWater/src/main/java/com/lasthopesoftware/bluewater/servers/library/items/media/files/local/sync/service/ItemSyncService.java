package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.connection.AccessConfiguration;
import com.lasthopesoftware.bluewater.servers.connection.AccessConfigurationBuilder;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionInfo;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.ActiveFileDownloadsActivity;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.receivers.SyncAlarmBroadcastReceiver;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;
import com.lasthopesoftware.threading.IOneParameterAction;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

/**
 * Created by david on 7/26/15.
 */
public class ItemSyncService extends Service {

	public static final String onFileDownloadedEvent = SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "onFileDownloadedEvent");
	public static final String onFileDownloadedStoreId = SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "onFileDownloadedStoreId");

	private static final String doSyncAction = SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "doSyncAction");
	private static final long syncInterval = 3 * 60 * 60 * 1000; // 3 hours

	private LocalBroadcastManager localBroadcastManager;
	private PowerManager.WakeLock wakeLock;

	private final Runnable finishServiceRunnable = new Runnable() {
		@Override
		public void run() {
			// Set an alarm for the next time we run this bad boy
			final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			final PendingIntent pendingIntent = PendingIntent.getBroadcast(ItemSyncService.this, 1, new Intent(SyncAlarmBroadcastReceiver.scheduledSyncIntent), PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, syncInterval, pendingIntent);

			stopForeground(true);
			stopSelf();
		}
	};

	private final IOneParameterAction<StoredFile> storedFileDownloadedAction = new IOneParameterAction<StoredFile>() {
		@Override
		public void run(StoredFile storedFile) {
			final Intent fileDownloadedIntent = new Intent(onFileDownloadedEvent);
			fileDownloadedIntent.putExtra(onFileDownloadedStoreId, storedFile.getId());
			localBroadcastManager.sendBroadcast(fileDownloadedIntent);
		}
	};

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
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!intent.getAction().equals(doSyncAction)) return START_REDELIVER_INTENT;

		final int result = START_NOT_STICKY;

		if (ConnectionInfo.getConnectionType(this) != ConnectivityManager.TYPE_WIFI) {
			stopSelf();
			return result;
		}

		final Intent batteryStatusReceiver = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		if (batteryStatusReceiver == null) {
			stopSelf();
			return result;
		}

		final int batteryStatus = batteryStatusReceiver.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		if (batteryStatus == 0) {
			stopSelf();
			return result;
		}

		final Context context = this;

		startForeground(23, buildSyncNotification());

		LibrarySession.GetLibraries(context, new ISimpleTask.OnCompleteListener<Void, Void, List<Library>>() {
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Library>> owner, final List<Library> libraries) {
				for (final Library library : libraries) {

					AccessConfigurationBuilder.buildConfiguration(context, library, new ISimpleTask.OnCompleteListener<Void, Void, AccessConfiguration>() {
						@Override
						public void onComplete(ISimpleTask<Void, Void, AccessConfiguration> owner, AccessConfiguration accessConfiguration) {
							if (library.isSyncLocalConnectionsOnly()) accessConfiguration.setLocalOnly(true);
							final ConnectionProvider connectionProvider = new ConnectionProvider(accessConfiguration);

							final StoredFileDownloader storedFileDownloader = new StoredFileDownloader(context, connectionProvider, library);
							storedFileDownloader.setOnFileDownloaded(storedFileDownloadedAction);
							storedFileDownloader.setOnFileQueueEmpty(finishServiceRunnable);

							LibrarySyncHandler.SyncLibrary(context, connectionProvider, library, storedFileDownloader);
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
