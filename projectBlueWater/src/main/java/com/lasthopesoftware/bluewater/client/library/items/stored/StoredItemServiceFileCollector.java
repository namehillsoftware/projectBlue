package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.stored.conversion.ConvertStoredPlaylistsToStoredItems;
import com.lasthopesoftware.bluewater.client.library.sync.CollectServiceFilesForSync;
import com.lasthopesoftware.bluewater.shared.observables.ObservedPromise;
import com.lasthopesoftware.bluewater.shared.observables.StreamedPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.propagation.RejectionProxy;
import com.namehillsoftware.handoff.promises.propagation.ResolutionProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

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
					return StreamedPromise
						.stream(storedPlaylistsToStoredItems.promiseConvertedStoredItem(storedItem))
						.flatMap(this::getStoredFilesStream);
				}

							return promiseServiceFiles(storedItem, cancellationProxy);
						});

					return Promise.whenAll(mappedFileDataPromises.toList());
				});

			cancellationProxy.doCancel(promisedServiceFileLists);

			promisedServiceFileLists
				.<Collection<ServiceFile>>then(serviceFiles -> Stream.of(serviceFiles).flatMap(Stream::of).collect(Collectors.toSet()))
				.then(new ResolutionProxy<>(serviceFileMessenger))
				.excuse(new RejectionProxy(serviceFileMessenger));
		});
	}

	private Promise<List<ServiceFile>> promiseServiceFiles(StoredItem storedItem, CancellationProxy cancellationProxy) {
		final int serviceId = storedItem.getServiceId();
		final Item item = new Item(serviceId);
		final String[] parameters = FileListParameters.getInstance().getFileListParameters(item);

		final Promise<List<ServiceFile>> serviceFilesPromise =
			fileProvider.promiseFiles(FileListParameters.Options.None, parameters);

		cancellationProxy.doCancel(serviceFilesPromise);

		return serviceFilesPromise
			.then(f -> f, e -> {
				if (e instanceof FileNotFoundException) {
					logger.warn("The item " + item.getKey() + " was not found, disabling sync for item");
					storedItemAccess.toggleSync(item, false);
					return Collections.emptyList();
				}

				throw e;
			});
	}
}
