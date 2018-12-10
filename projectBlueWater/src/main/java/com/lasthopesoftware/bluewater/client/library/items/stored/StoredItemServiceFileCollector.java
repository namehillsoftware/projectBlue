package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.sync.CollectServiceFilesForSync;
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
	private final IFileProvider fileProvider;

	public StoredItemServiceFileCollector(IStoredItemAccess storedItemAccess, IFileProvider fileProvider) {
		this.storedItemAccess = storedItemAccess;
		this.fileProvider = fileProvider;
	}

	@Override
	public Promise<Collection<ServiceFile>> promiseServiceFilesToSync() {
		return new Promise<>(serviceFileMessenger -> {
			final CancellationProxy cancellationProxy = new CancellationProxy();
			serviceFileMessenger.cancellationRequested(cancellationProxy);

			final Promise<Collection<StoredItem>> promisedStoredItems = storedItemAccess.promiseStoredItems();
			cancellationProxy.doCancel(promisedStoredItems);

			final Promise<Collection<List<ServiceFile>>> promisedServiceFileLists = promisedStoredItems
				.eventually(storedItems -> {
					if (cancellationProxy.isCancelled())
						return new Promise<>(new CancellationException());

					final Stream<Promise<List<ServiceFile>>> mappedFileDataPromises = Stream.of(storedItems)
						.map(storedItem -> {
							if (storedItem.getItemType() == StoredItem.ItemType.PLAYLIST) {

							}

							final int serviceId = storedItem.getServiceId();
							//TODO migrate synchronized playlist ID's to item ID's
							final Item item = new Item(serviceId);
							final String[] parameters = new FileListParameters().getFileListParameters(item);

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
}
