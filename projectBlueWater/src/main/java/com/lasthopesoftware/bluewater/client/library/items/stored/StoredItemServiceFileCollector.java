package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.sync.IServiceFilesToSyncCollector;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.aggregation.AggregateCancellation;
import com.lasthopesoftware.messenger.promises.aggregation.CollectedErrorExcuse;
import com.lasthopesoftware.messenger.promises.aggregation.CollectedResultsResolver;
import com.lasthopesoftware.messenger.promises.propagation.CancellationProxy;
import com.lasthopesoftware.messenger.promises.propagation.RejectionProxy;
import com.lasthopesoftware.messenger.promises.propagation.ResolutionProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;

import static com.lasthopesoftware.messenger.promises.response.ImmediateAction.perform;


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
		return new Promise<>(storedItemsMessenger -> {
			final CancellationProxy cancellationProxy = new CancellationProxy();
			storedItemsMessenger.cancellationRequested(cancellationProxy);

			final Promise<Collection<StoredItem>> promisedStoredItems = storedItemAccess.promiseStoredItems();
			cancellationProxy.doCancel(promisedStoredItems);

			final Promise<Collection<List<ServiceFile>>> promisedServiceFileLists = promisedStoredItems
				.eventually(storedItems -> new Promise<>(messenger -> {
					if (cancellationProxy.isCancelled()) {
						messenger.sendRejection(new CancellationException());
						return;
					}

					final Stream<Promise<List<ServiceFile>>> mappedFileDataPromises = Stream.of(storedItems)
						.map(storedItem -> {
							final int serviceId = storedItem.getServiceId();
							final String[] parameters = (storedItem.getItemType() == StoredItem.ItemType.ITEM ? new Item(serviceId) : new Playlist(serviceId)).getFileListParameters();

							final Promise<List<ServiceFile>> serviceFileListPromise = fileProvider.promiseFiles(FileListParameters.Options.None, parameters);
							serviceFileListPromise
								.excuse(perform(e -> {
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
						new CollectedErrorExcuse<List<ServiceFile>>(messenger, promises) {
							@Override
							public Throwable respond(Throwable throwable) throws Exception {
								return (throwable instanceof FileNotFoundException) ? throwable : super.respond(throwable);
							}
						};
					if (collectedErrorExcuse.isRejected()) return;

					final CollectedResultsResolver<List<ServiceFile>> collectedResultsResolver = new CollectedResultsResolver<>(messenger, promises);
					messenger.cancellationRequested(new AggregateCancellation<>(messenger, promises, collectedResultsResolver));
				}));

			cancellationProxy.doCancel(promisedServiceFileLists);

			promisedServiceFileLists
				.<Collection<ServiceFile>>then(serviceFiles -> Stream.of(serviceFiles).flatMap(Stream::of).toList())
				.then(new ResolutionProxy<>(storedItemsMessenger))
				.excuse(new RejectionProxy(storedItemsMessenger));
		});
	}
}
