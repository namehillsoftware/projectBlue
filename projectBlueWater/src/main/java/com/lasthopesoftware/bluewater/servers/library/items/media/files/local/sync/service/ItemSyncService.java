package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredItemAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.store.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.store.StoredItem;
import com.lasthopesoftware.bluewater.servers.store.Library;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by david on 7/26/15.
 */
public class ItemSyncService extends Service {

	public static final String onFileDownloadedEvent = SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "onFileDownloadedEvent");
	public static final String onFileDownloadedStoreId = SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "onFileDownloadedStoreId");

	private static final String doSyncAction = SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "doSyncAction");

	private LocalBroadcastManager mLocalBroadcastManager;

	public static void doSync(Context context) {
		final Intent intent = new Intent(context, ItemSyncService.class);
		intent.setAction(doSyncAction);

		context.startService(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
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

		final StoredItemAccess storedItemAccess = new StoredItemAccess(context);
		storedItemAccess.getAllStoredItems(new ISimpleTask.OnCompleteListener<Void, Void, List<StoredItem>>() {
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<StoredItem>> owner, final List<StoredItem> storedItems) {
				LibrarySession.GetActiveLibrary(context, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {
					@Override
					public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library library) {
						AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
							@Override
							public void run() {
								final Set<Integer> allSyncedFileKeys = new HashSet<>();
								final StoredFileAccess storedFileAccess = new StoredFileAccess(context, library);
								final StoredFileDownloader storedFileDownloader = new StoredFileDownloader(context, library);
								storedFileDownloader.setOnFileQueueEmpty(new StoredFileDownloader.OnFileQueueEmpty() {
									@Override
									public void onFileQueueEmpty() {
										stopForeground(true);
										stopSelf();
									}
								});

								storedFileDownloader.setOnFileDownloaded(new StoredFileDownloader.OnFileDownloaded() {
									@Override
									public void onFileDownloaded(StoredFile storedFile) {
										final Intent fileDownloadedIntent = new Intent(onFileDownloadedEvent);
										fileDownloadedIntent.putExtra(onFileDownloadedStoreId, storedFile.getId());
										mLocalBroadcastManager.sendBroadcast(fileDownloadedIntent);
									}
								});

								for (StoredItem listToSync : storedItems) {
									final int serviceId = listToSync.getServiceId();
									final IFilesContainer filesContainer = listToSync.getItemType() == StoredItem.ItemType.ITEM ? new Item(serviceId) : new Playlist(serviceId);
									final ArrayList<IFile> files = filesContainer.getFiles().getFiles();
									for (final IFile file : files) {
										allSyncedFileKeys.add(file.getKey());

										storedFileAccess.createOrUpdateFile(file, new ISimpleTask.OnCompleteListener<Void, Void, StoredFile>() {
											@Override
											public void onComplete(ISimpleTask<Void, Void, StoredFile> owner, StoredFile storedFile) {
												if (!storedFile.isDownloadComplete())
													storedFileDownloader.queueFileForDownload(file, storedFile);
											}
										});
									}
								}

								storedFileAccess.pruneStoredFiles(allSyncedFileKeys);
							}
						});
					}
				});
			}
		});
	}

	private Notification buildSyncNotification() {
		final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this);
		notifyBuilder.setSmallIcon(R.drawable.clearstream_logo_dark);
		notifyBuilder.setContentTitle("Syncing files");
		return notifyBuilder.build();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class ItemSyncServiceBinder extends Binder {
		ItemSyncService getService() {
			return ItemSyncService.this;
		}
	}

	private final IBinder mBinder = new ItemSyncServiceBinder();
}
