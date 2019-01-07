package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.stored.conversion.ConvertStoredPlaylistsToStoredItems;
import com.lasthopesoftware.bluewater.client.library.sync.CollectServiceFilesForSync;
import com.lasthopesoftware.bluewater.shared.observables.ObservedPromise;
import com.namehillsoftware.handoff.promises.Promise;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

public class StoredItemServiceFileCollector implements CollectServiceFilesForSync {

	private static final Logger logger = LoggerFactory.getLogger(StoredItemServiceFileCollector.class);

	private final IStoredItemAccess storedItemAccess;
	private final ConvertStoredPlaylistsToStoredItems storedPlaylistsToStoredItems;
	private final IFileProvider fileProvider;

	public StoredItemServiceFileCollector(IStoredItemAccess storedItemAccess, ConvertStoredPlaylistsToStoredItems storedPlaylistsToStoredItems, IFileProvider fileProvider) {
		this.storedItemAccess = storedItemAccess;
		this.storedPlaylistsToStoredItems = storedPlaylistsToStoredItems;
		this.fileProvider = fileProvider;
	}

	@Override
	public Observable<ServiceFile> streamServiceFilesToSync() {
		return getStoredItemStream()
			.flatMap(storedItem -> {
				if (storedItem.getItemType() == StoredItem.ItemType.PLAYLIST) {
					return ObservedPromise
						.observe(storedPlaylistsToStoredItems.promiseConvertedStoredItem(storedItem))
						.flatMap(this::getStoredFilesStream);
				}

				return getStoredFilesStream(storedItem);
			});
	}

	private Observable<StoredItem> getStoredItemStream() {
		return ObservedPromise.observe(storedItemAccess.promiseStoredItems()).flatMap(Observable::fromIterable);
	}

	private Observable<ServiceFile> getStoredFilesStream(StoredItem storedItem) {
		final int serviceId = storedItem.getServiceId();
		final Item item = new Item(serviceId);
		final String[] parameters = FileListParameters.getInstance().getFileListParameters(item);

		final Promise<List<ServiceFile>> serviceFilesPromise =
			fileProvider.promiseFiles(FileListParameters.Options.None, parameters);

//		cancellationProxy.doCancel(serviceFilesPromise);

		return ObservedPromise.observe(serviceFilesPromise
			.then(f -> f, e -> {
				if (e instanceof FileNotFoundException) {
					logger.warn("The item " + item.getKey() + " was not found, disabling sync for item");
					storedItemAccess.toggleSync(item, false);
					return Collections.<ServiceFile>emptyList();
				}

				throw e;
			})).flatMap(Observable::fromIterable);
	}
}
