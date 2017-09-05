package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.sync.IServiceFilesToSyncCollector;
import com.lasthopesoftware.messenger.promises.Promise;

import java.util.Collection;
import java.util.List;


public class StoredItemServiceFileCollector implements IServiceFilesToSyncCollector {

	private final IStoredItemAccess storedItemAccess;
	private final IFileProvider fileProvider;

	public StoredItemServiceFileCollector(IStoredItemAccess storedItemAccess, IFileProvider fileProvider) {
		this.storedItemAccess = storedItemAccess;
		this.fileProvider = fileProvider;
	}

	@Override
	public Promise<Collection<ServiceFile>> promiseServiceFilesToSync() {
		return storedItemAccess
			.promiseStoredItems()
			.eventually(storedItems -> {
				final Stream<Promise<List<ServiceFile>>> mappedFileDataPromises = Stream.of(storedItems)
					.map(storedItem -> {
						final int serviceId = storedItem.getServiceId();
						final String[] parameters = (storedItem.getItemType() == StoredItem.ItemType.ITEM ? new Item(serviceId) : new Playlist(serviceId)).getFileListParameters();

						final Promise<List<ServiceFile>> serviceFileListPromise = fileProvider.promiseFiles(FileListParameters.Options.None, parameters);

						return serviceFileListPromise;
					});

				return Promise.whenAll(mappedFileDataPromises.toList()).then(serviceFiles -> Stream.of(serviceFiles).flatMap(Stream::of).toList());
			});
	}
}
