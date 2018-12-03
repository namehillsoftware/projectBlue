package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.sync.IServiceFilesToSyncCollector;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.aggregation.AggregateCancellation;
import com.namehillsoftware.handoff.promises.aggregation.CollectedErrorExcuse;
import com.namehillsoftware.handoff.promises.aggregation.CollectedResultsResolver;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.propagation.RejectionProxy;
import com.namehillsoftware.handoff.promises.propagation.ResolutionProxy;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;

public class StoredItemServiceFileCollector implements IServiceFilesToSyncCollector {

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
				.eventually(storedItems -> new Promise<>(storedItemsMessenger -> {
					if (cancellationProxy.isCancelled()) {
						storedItemsMessenger.sendRejection(new CancellationException());
						return;
					}

					final Stream<Promise<List<ServiceFile>>> mappedFileDataPromises = Stream.of(storedItems)
						.map(storedItem -> {
							final int serviceId = storedItem.getServiceId();
							final String[] parameters = new FileListParameters().getFileListParameters(new Item(serviceId));

							final Promise<List<ServiceFile>> serviceFileListPromise = fileProvider.promiseFiles(FileListParameters.Options.None, parameters);
							serviceFileListPromise
								.excuse(new VoidResponse<>(e -> {
									if (e instanceof FileNotFoundException) {
										final IItem item = storedItem.getItemType() == StoredItem.ItemType.ITEM ? new Item(serviceId) : new Playlist(serviceId);
										logger.warn("The item " + item.getKey() + " was not found, disabling sync for item");
										storedItemAccess.toggleSync(item, false);
										return;
									}

									throw e;
								}));

							return serviceFileListPromise;
						});

					final List<Promise<List<ServiceFile>>> promises = mappedFileDataPromises.toList();
					final CollectedErrorExcuse<List<ServiceFile>> collectedErrorExcuse =
						new CollectedErrorExcuse<List<ServiceFile>>(storedItemsMessenger, promises) {
							@Override
							public Throwable respond(Throwable throwable) throws Exception {
								return (throwable instanceof FileNotFoundException) ? throwable : super.respond(throwable);
							}
						};
					if (collectedErrorExcuse.isRejected()) return;

					final CollectedResultsResolver<List<ServiceFile>> collectedResultsResolver = new CollectedResultsResolver<>(storedItemsMessenger, promises);
					storedItemsMessenger.cancellationRequested(new AggregateCancellation<>(storedItemsMessenger, promises, collectedResultsResolver));
				}));

			cancellationProxy.doCancel(promisedServiceFileLists);

			promisedServiceFileLists
				.<Collection<ServiceFile>>then(serviceFiles -> Stream.of(serviceFiles).flatMap(Stream::of).collect(Collectors.toSet()))
				.then(new ResolutionProxy<>(serviceFileMessenger))
				.excuse(new RejectionProxy(serviceFileMessenger));
		});
	}
}
