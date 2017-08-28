package com.lasthopesoftware.bluewater.client.library.sync;

import android.content.Context;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.FileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResult;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.LibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.LibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.lasthopesoftware.messenger.promises.response.ImmediateAction.perform;

public class LibrarySyncHandler {

	private static final Logger logger = LoggerFactory.getLogger(LibrarySyncHandler.class);

	private final IConnectionProvider connectionProvider;
	private final FileProvider fileProvider;
	private final Library library;
	private final ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider;
	private final ILibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider;
	private final StoredItemAccess storedItemAccess;
	private final StoredFileAccess storedFileAccess;
	private final StoredFileDownloader storedFileDownloader;
	private OneParameterAction<LibrarySyncHandler> onQueueProcessingCompleted;

	private volatile boolean isCancelled;

	public LibrarySyncHandler(Context context, IConnectionProvider connectionProvider, Library library) {
		this(
			connectionProvider,
			library,
			new StoredItemAccess(context, library),
			new StoredFileAccess(context, library),
			new StoredFileDownloader(context, connectionProvider, library),
			new FileProvider(new FileStringListProvider(connectionProvider)),
			new LibraryStorageReadPermissionsRequirementsProvider(),
			new LibraryStorageWritePermissionsRequirementsProvider());
	}

	private LibrarySyncHandler(IConnectionProvider connectionProvider, Library library, StoredItemAccess storedItemAccess, StoredFileAccess storedFileAccess, StoredFileDownloader storedFileDownloader, FileProvider fileProvider, ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider, ILibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider) {
		this.connectionProvider = connectionProvider;
		this.library = library;
		this.storedItemAccess = storedItemAccess;
		this.storedFileAccess = storedFileAccess;
		this.storedFileDownloader = storedFileDownloader;
		this.fileProvider = fileProvider;
		this.libraryStorageReadPermissionsRequirementsProvider = libraryStorageReadPermissionsRequirementsProvider;
		this.libraryStorageWritePermissionsRequirementsProvider = libraryStorageWritePermissionsRequirementsProvider;
	}

	public void setOnFileDownloading(OneParameterAction<StoredFile> onFileDownloading) {
		storedFileDownloader.setOnFileDownloading(onFileDownloading);
	}

	public void setOnFileDownloaded(OneParameterAction<StoredFileJobResult> onFileDownloaded) {
		storedFileDownloader.setOnFileDownloaded(onFileDownloaded);
	}

	public void setOnFileQueued(OneParameterAction<StoredFile> onFileQueued) {
		storedFileDownloader.setOnFileQueued(onFileQueued);
	}

	public void setOnQueueProcessingCompleted(final OneParameterAction<LibrarySyncHandler> onQueueProcessingCompleted) {
		this.onQueueProcessingCompleted = onQueueProcessingCompleted;
	}

	public void setOnFileReadError(TwoParameterAction<Library, StoredFile> onFileReadError) {
		storedFileDownloader.setOnFileReadError(storedFile -> {
			if (libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(library))
				onFileReadError.runWith(library, storedFile);
		});
	}

	public void setOnFileWriteError(TwoParameterAction<Library, StoredFile> onFileWriteError) {
		storedFileDownloader.setOnFileWriteError(storedFile -> {
			if (libraryStorageWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(library))
				onFileWriteError.runWith(library, storedFile);
		});
	}

	public void cancel() {
		isCancelled = true;

		storedFileDownloader.cancel();
	}

	public void startSync() {
		storedItemAccess
			.getStoredItems()
			.eventually(storedItems -> {
				if (isCancelled) {
					handleQueueProcessingCompleted();
					return Promise.empty();
				}

				final Stream<Promise<List<ServiceFile>>> mappedFileDataPromises = Stream.of(storedItems)
					.map(storedItem -> {
						if (isCancelled) {
							handleQueueProcessingCompleted();
							return new Promise<>(Collections.emptyList());
						}

						final int serviceId = storedItem.getServiceId();
						final String[] parameters = (storedItem.getItemType() == StoredItem.ItemType.ITEM ? new Item(serviceId) : new Playlist(serviceId)).getFileListParameters();

						final Promise<List<ServiceFile>> serviceFileListPromise = fileProvider.promiseFiles(FileListParameters.Options.None, parameters);
						serviceFileListPromise
							.excuse(perform(e -> {
								if (e instanceof FileNotFoundException) {
									final IItem item = storedItem.getItemType() == StoredItem.ItemType.ITEM ? new Item(serviceId) : new Playlist(serviceId);
									logger.warn("The item " + item.getKey() + " was not found, disabling sync for item");
									storedItemAccess.toggleSync(item, false);
								}
							}));

						return serviceFileListPromise;
					});

				return Promise.whenAll(mappedFileDataPromises.toList())
					.then(manyServiceFiles -> Stream.of(manyServiceFiles).flatMap(Stream::of).collect(Collectors.toSet()))
					.eventually(allServiceFilesToSync -> {
						final Promise<Collection<Void>> pruneFilesTask = storedFileAccess.pruneStoredFiles(Stream.of(allServiceFilesToSync).map(ServiceFile::getKey).collect(Collectors.toSet()));
						pruneFilesTask.excuse(perform(e -> logger.warn("There was an error pruning the files", e)));

						return !isCancelled
							? pruneFilesTask.then(voids -> allServiceFilesToSync)
							: new Promise<Set<ServiceFile>>(Collections.emptySet());
					})
					.eventually(allServiceFilesToSync -> {
						if (isCancelled)
							return new Promise<>(Collections.emptySet());

						final List<Promise<StoredFile>> upsertFiles = Stream.of(allServiceFilesToSync)
							.map(serviceFile -> {
								if (isCancelled)
									return new Promise<>((StoredFile) null);

								return storedFileAccess
									.createOrUpdateFile(connectionProvider, serviceFile)
									.then(new DownloadGuard(storedFileDownloader, serviceFile));
							})
							.toList();

						return Promise.whenAll(upsertFiles);
					})
					.then(vs -> {
						storedFileDownloader.setOnQueueProcessingCompleted(this::handleQueueProcessingCompleted);

						if (!isCancelled)
							storedFileDownloader.process();
						else
							handleQueueProcessingCompleted();

						return null;
					});
			})
			.excuse(e -> {
				logger.warn("There was an error retrieving the files", e);

				handleQueueProcessingCompleted();

				return null;
			});;
	}

	private void handleQueueProcessingCompleted() {
		if (onQueueProcessingCompleted != null)
			onQueueProcessingCompleted.runWith(this);
	}

	private static class DownloadGuard implements ImmediateResponse<StoredFile, StoredFile> {
		private final ServiceFile serviceFile;
		private StoredFileDownloader storedFileDownloader;

		DownloadGuard(StoredFileDownloader storedFileDownloader, ServiceFile serviceFile) {
			this.storedFileDownloader = storedFileDownloader;
			this.serviceFile = serviceFile;
		}

		@Override
		public StoredFile respond(StoredFile storedFile) {
			if (storedFile != null && !storedFile.isDownloadComplete())
				storedFileDownloader.queueFileForDownload(serviceFile, storedFile);

			return storedFile;
		}
	}
}
