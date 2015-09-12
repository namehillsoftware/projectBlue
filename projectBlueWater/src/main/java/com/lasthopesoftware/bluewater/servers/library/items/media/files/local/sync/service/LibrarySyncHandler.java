package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.service;

import android.content.Context;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredItemAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.repository.StoredItem;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by david on 8/30/15.
 */
public class LibrarySyncHandler implements Runnable {

	private final Context context;
	private final ConnectionProvider connectionProvider;
	private final Library library;
	private final List<StoredItem> storedItems;
	private final StoredFileDownloader storedFileDownloader;

	public static void SyncLibrary(final Context context, final ConnectionProvider connectionProvider, final Library library, final StoredFileDownloader storedFileDownloader) {
		final StoredItemAccess storedItemAccess = new StoredItemAccess(context, library);
		storedItemAccess.getStoredItems(new ISimpleTask.OnCompleteListener<Void, Void, List<StoredItem>>() {

			@Override
			public void onComplete(ISimpleTask<Void, Void, List<StoredItem>> owner, List<StoredItem> storedItems) {
				AsyncTask
					.THREAD_POOL_EXECUTOR
					.execute(new LibrarySyncHandler(context, connectionProvider, library, storedItems, storedFileDownloader));
			}
		});
	}

	private LibrarySyncHandler(Context context, ConnectionProvider connectionProvider, Library library, List<StoredItem> storedItems, StoredFileDownloader storedFileDownloader) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.library = library;
		this.storedItems = storedItems;
		this.storedFileDownloader = storedFileDownloader;
	}

	@Override
	public void run() {
		final Set<Integer> allSyncedFileKeys = new HashSet<>();
		final StoredFileAccess storedFileAccess = new StoredFileAccess(context, library);

		for (StoredItem storedItem : storedItems) {
			final int serviceId = storedItem.getServiceId();
			final IFilesContainer filesContainer = storedItem.getItemType() == StoredItem.ItemType.ITEM ? new Item(connectionProvider, serviceId) : new Playlist(connectionProvider, serviceId);
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
}
