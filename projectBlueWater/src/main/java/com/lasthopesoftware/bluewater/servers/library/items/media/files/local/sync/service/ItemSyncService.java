package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

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

	private static final String doSyncAction = SpecialValueHelpers.buildMagicPropertyName(ItemSyncService.class, "doSyncAction");

	public static void doSync(Context context) {
		final Intent intent = new Intent(context, ItemSyncService.class);
		intent.setAction(doSyncAction);

		context.startService(intent);
	}

	private void doSync() {
		final Context context = this;
		LibrarySession.GetActiveLibrary(this, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {
			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library library) {
				final StoredItemAccess storedItemAccess = new StoredItemAccess(context);
				storedItemAccess.getAllStoredItems(new ISimpleTask.OnCompleteListener<Void, Void, List<StoredItem>>() {
					@Override
					public void onComplete(ISimpleTask<Void, Void, List<StoredItem>> owner, final List<StoredItem> storedItems) {
						AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
							@Override
							public void run() {
								final Set<Integer> allSyncedFileKeys = new HashSet<>();
								final StoredFileAccess storedFileAccess = new StoredFileAccess(context, library);
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
													StoreFilesService.queueFileForDownload(context, file, storedFile);
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
