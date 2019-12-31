package com.lasthopesoftware.bluewater.client.stored.library.items;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.ProvideFiles;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.sync.CollectServiceFilesForSync;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.propagation.RejectionProxy;
import com.namehillsoftware.handoff.promises.propagation.ResolutionProxy;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

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
	private final ProvideFiles fileProvider;
	private final IFileListParameterProvider fileListParameters;

	public StoredItemServiceFileCollector(
		IStoredItemAccess storedItemAccess,
		ProvideFiles fileProvider,
		IFileListParameterProvider fileListParameters) {
		this.storedItemAccess = storedItemAccess;
		this.fileProvider = fileProvider;
		this.fileListParameters = fileListParameters;
	}

	@Override
	public Promise<Collection<ServiceFile>> promiseServiceFilesToSync(LibraryId libraryId) {
		return new Promise<>(serviceFileMessenger -> {
			final CancellationProxy cancellationProxy = new CancellationProxy();
			serviceFileMessenger.cancellationRequested(cancellationProxy);

			final Promise<Collection<StoredItem>> promisedStoredItems = storedItemAccess.promiseStoredItems(libraryId);
			cancellationProxy.doCancel(promisedStoredItems);

			final Promise<Collection<Collection<ServiceFile>>> promisedServiceFileLists = promisedStoredItems
				.eventually(storedItems -> {
					if (cancellationProxy.isCancelled())
						return new Promise<>(new CancellationException());

					final Stream<Promise<Collection<ServiceFile>>> mappedFileDataPromises = Stream.of(storedItems)
						.map(storedItem -> promiseServiceFiles(libraryId, storedItem, cancellationProxy));

					return Promise.whenAll(mappedFileDataPromises.toList());
				});

			cancellationProxy.doCancel(promisedServiceFileLists);

			promisedServiceFileLists
				.<Collection<ServiceFile>>then(serviceFiles -> Stream.of(serviceFiles).flatMap(Stream::of).collect(Collectors.toSet()))
				.then(new ResolutionProxy<>(serviceFileMessenger), new RejectionProxy(serviceFileMessenger));
		});
	}

	private Promise<Collection<ServiceFile>> promiseServiceFiles(LibraryId libraryId, StoredItem storedItem, CancellationProxy cancellationProxy) {
		switch (storedItem.getItemType()) {
			case ITEM:
				return promiseServiceFiles(libraryId, new Item(storedItem.getServiceId()), cancellationProxy);
			case PLAYLIST:
				return promiseServiceFiles(libraryId, new Playlist(storedItem.getServiceId()), cancellationProxy);
			default:
				return new Promise<>(Collections.emptyList());
		}
	}

	private Promise<Collection<ServiceFile>> promiseServiceFiles(LibraryId libraryId, Item item, CancellationProxy cancellationProxy) {
		final String[] parameters = fileListParameters.getFileListParameters(item);

		final Promise<List<ServiceFile>> serviceFilesPromise = fileProvider.promiseFiles(libraryId, FileListParameters.Options.None, parameters);

		cancellationProxy.doCancel(serviceFilesPromise);

		return serviceFilesPromise.excuse(new ExceptionHandler(libraryId, item, storedItemAccess));
	}

	private Promise<Collection<ServiceFile>> promiseServiceFiles(LibraryId libraryId, Playlist playlist, CancellationProxy cancellationProxy) {
		final String[] parameters = fileListParameters.getFileListParameters(playlist);

		final Promise<List<ServiceFile>> serviceFilesPromise = fileProvider.promiseFiles(libraryId, FileListParameters.Options.None, parameters);

		cancellationProxy.doCancel(serviceFilesPromise);

		return serviceFilesPromise.excuse(new ExceptionHandler(libraryId, playlist, storedItemAccess));
	}

	private static class ExceptionHandler implements ImmediateResponse<Throwable, Collection<ServiceFile>> {
		private final LibraryId libraryId;
		private final IItem item;
		private final IStoredItemAccess storedItemAccess;

		ExceptionHandler(LibraryId libraryId, IItem item, IStoredItemAccess storedItemAccess) {
			this.libraryId = libraryId;
			this.item = item;
			this.storedItemAccess = storedItemAccess;
		}

		@Override
		public List<ServiceFile> respond(Throwable e) throws Throwable {
			if (e instanceof FileNotFoundException) {
				logger.warn("The item " + item.getKey() + " was not found, disabling sync for item");
				storedItemAccess.toggleSync(libraryId, item, false);
				return Collections.emptyList();
			}

			throw e;
		}
	}
}
