package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredItemAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.ActiveFileDownloadsActivity;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.receivers.SyncAlarmBroadcastReceiver;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.repository.StoredItem;
import com.lasthopesoftware.bluewater.servers.repository.Library;
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

		doSync();
		return START_STICKY;
	}

	private void doSync() {
		final Context context = this;

		startForeground(23, buildSyncNotification());

		LibrarySession.GetLibraries(context, new ISimpleTask.OnCompleteListener<Void, Void, List<Library>>() {
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Library>> owner, final List<Library> libraries) {
				for (final Library library : libraries) {
					final StoredItemAccess storedItemAccess = new StoredItemAccess(context, library);
					storedItemAccess.getStoredItems(new ISimpleTask.OnCompleteListener<Void, Void, List<StoredItem>>() {
						@Override
						public void onComplete(ISimpleTask<Void, Void, List<StoredItem>> owner, List<StoredItem> storedItems) {
							final StoredFileDownloader storedFileDownloader = new StoredFileDownloader(context, library);
							storedFileDownloader.setOnFileDownloaded(storedFileDownloadedAction);
							storedFileDownloader.setOnFileQueueEmpty(finishServiceRunnable);

							AsyncTask.THREAD_POOL_EXECUTOR.execute(new LibrarySyncRunnable(context, SessionConnection.getSessionConnectionProvider(), library, storedItems, storedFileDownloader));
						}
					});
				}
			}
		});
	}

	private Notification buildSyncNotification() {
		final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this);
		notifyBuilder.setSmallIcon(R.drawable.clearstream_logo_dark);
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
