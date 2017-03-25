package com.lasthopesoftware.bluewater.client.library.sync;

import android.content.Context;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.FileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileListParameterProvider;
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
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

/**
 * Created by david on 8/30/15.
 */
public class LibrarySyncHandler {

	private static final Logger logger = LoggerFactory.getLogger(LibrarySyncHandler.class);

	private final Context context;
	private final ConnectionProvider connectionProvider;
	private final Library library;
	private final ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider;
	private final ILibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider;
	private final StoredFileDownloader storedFileDownloader;
	private OneParameterAction<LibrarySyncHandler> onQueueProcessingCompleted;

	private volatile boolean isCancelled;
	private volatile boolean isFaulted;

	public LibrarySyncHandler(Context context, ConnectionProvider connectionProvider, Library library) {
		this(
				context,
				connectionProvider,
				library,
				new LibraryStorageReadPermissionsRequirementsProvider(),
				new LibraryStorageWritePermissionsRequirementsProvider());
	}

	public LibrarySyncHandler(Context context, ConnectionProvider connectionProvider, Library library, ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider, ILibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.library = library;
		this.libraryStorageReadPermissionsRequirementsProvider = libraryStorageReadPermissionsRequirementsProvider;
		this.libraryStorageWritePermissionsRequirementsProvider = libraryStorageWritePermissionsRequirementsProvider;
		this.storedFileDownloader = new StoredFileDownloader(context, connectionProvider, library);
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
		final StoredItemAccess storedItemAccess = new StoredItemAccess(context, library);
		storedItemAccess
			.getStoredItems()
			.onComplete((storedItems) -> AsyncTask
				.THREAD_POOL_EXECUTOR
				.execute(() -> {
					if (isCancelled) {
						handleQueueProcessingCompleted();
						return;
					}

					final Set<Integer> allSyncedFileKeys = new HashSet<>();
					final StoredFileAccess storedFileAccess = new StoredFileAccess(context, library);

					final Queue<Map.Entry<IItem, FileProvider>> fileProviders = new LinkedList<>();
					for (StoredItem storedItem : storedItems) {
						if (isCancelled) {
							handleQueueProcessingCompleted();
							return;
						}

						final int serviceId = storedItem.getServiceId();
						final IItem item = storedItem.getItemType() == StoredItem.ItemType.ITEM ? new Item(serviceId) : new Playlist(serviceId);
						final FileProvider fileProvider = new FileProvider(connectionProvider, (IFileListParameterProvider) item);
						fileProvider.execute();
						fileProviders.offer(new AbstractMap.SimpleImmutableEntry<>(item, fileProvider));
					}

					final List<IPromise<Void>> upsertStoredFilePromises = new ArrayList<>(fileProviders.size());

					Map.Entry<IItem, FileProvider> fileProviderEntry;
					while ((fileProviderEntry = fileProviders.poll()) != null) {
						if (isCancelled) {
							handleQueueProcessingCompleted();
							return;
						}

						final IItem item = fileProviderEntry.getKey();
						final FileProvider fileProvider = fileProviderEntry.getValue();

						try {
							final List<IFile> files = fileProvider.get();
							for (final IFile file : files) {
								allSyncedFileKeys.add(file.getKey());

								if (isCancelled) {
									handleQueueProcessingCompleted();
									return;
								}

								final IPromise<Void> upsertStoredFilePromise =
									storedFileAccess
										.createOrUpdateFile(connectionProvider, file)
										.then(runCarelessly(storedFile -> {
											if (storedFile != null && !storedFile.isDownloadComplete())
												storedFileDownloader.queueFileForDownload(file, storedFile);
										}));

								upsertStoredFilePromises.add(upsertStoredFilePromise);
							}
						} catch (ExecutionException | InterruptedException e) {

							if (e.getCause() instanceof FileNotFoundException) {
								logger.warn("The item " + item.getKey() + " was not found, disabling sync for item");
								storedItemAccess.toggleSync(item, false);
								continue;
							}

							isFaulted = true;
							logger.warn("There was an error retrieving the files", e);

							if (isCancelled) {
								handleQueueProcessingCompleted();
								return;
							}
						}
					}

					storedFileDownloader.setOnQueueProcessingCompleted(() -> {
						if (!isCancelled && !isFaulted)
							storedFileAccess.pruneStoredFiles(allSyncedFileKeys);

						handleQueueProcessingCompleted();
					});

					if (isCancelled) {
						handleQueueProcessingCompleted();
						return;
					}

					Promise.whenAll(upsertStoredFilePromises)
						.then(runCarelessly(files -> storedFileDownloader.process()));
				}));
	}

	private void handleQueueProcessingCompleted() {
		if (onQueueProcessingCompleted != null)
			onQueueProcessingCompleted.runWith(LibrarySyncHandler.this);
	}
}
