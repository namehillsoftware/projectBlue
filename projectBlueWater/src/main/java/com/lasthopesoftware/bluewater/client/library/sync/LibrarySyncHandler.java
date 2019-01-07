package com.lasthopesoftware.bluewater.client.library.sync;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.IStoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.updates.UpdateStoredFiles;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.vedsoft.futures.runnables.OneParameterAction;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LibrarySyncHandler {

	private static final Logger logger = LoggerFactory.getLogger(LibrarySyncHandler.class);

	private final ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider;
	private final ILibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider;
	private final CollectServiceFilesForSync serviceFilesToSyncCollector;
	private final IStoredFileAccess storedFileAccess;
	private final UpdateStoredFiles storedFileUpdater;
	private final IStoredFileDownloader storedFileDownloader;
	private OneParameterAction<LibrarySyncHandler> onQueueProcessingCompleted;

	private final CancellationProxy cancellationProxy = new CancellationProxy();

	public LibrarySyncHandler(
		CollectServiceFilesForSync serviceFilesToSyncCollector,
		IStoredFileAccess storedFileAccess,
		UpdateStoredFiles storedFileUpdater,
		IStoredFileDownloader storedFileDownloader,
		ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider,
		ILibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider) {
		this.serviceFilesToSyncCollector = serviceFilesToSyncCollector;
		this.storedFileAccess = storedFileAccess;
		this.storedFileUpdater = storedFileUpdater;
		this.storedFileDownloader = storedFileDownloader;
		this.libraryStorageReadPermissionsRequirementsProvider = libraryStorageReadPermissionsRequirementsProvider;
		this.libraryStorageWritePermissionsRequirementsProvider = libraryStorageWritePermissionsRequirementsProvider;
	}

	public void setOnFileDownloading(OneParameterAction<StoredFile> onFileDownloading) {
		storedFileDownloader.setOnFileDownloading(onFileDownloading);
	}

	public void setOnFileDownloaded(OneParameterAction<StoredFileJobStatus> onFileDownloaded) {
//		storedFileDownloader.setOnFileDownloaded(onFileDownloaded);
	}

	public void setOnFileQueued(OneParameterAction<StoredFile> onFileQueued) {
//		storedFileDownloader.setOnFileQueued(onFileQueued);
	}

	public void setOnQueueProcessingCompleted(final OneParameterAction<LibrarySyncHandler> onQueueProcessingCompleted) {
		this.onQueueProcessingCompleted = onQueueProcessingCompleted;
	}

//	public void setOnFileReadError(TwoParameterAction<Library, StoredFile> onFileReadError) {
//		storedFileDownloader.setOnFileReadError(storedFile -> {
//			if (libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(library))
//				onFileReadError.runWith(library, storedFile);
//		});
//	}
//
//	public void setOnFileWriteError(OneParameterAction<Library, StoredFile> onFileWriteError) {
//		storedFileDownloader.setOnFileWriteError(storedFile -> {
//			if (libraryStorageWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(library))
//				onFileWriteError.runWith(library, storedFile);
//		});
//	}

	public void cancel() {
		cancellationProxy.run();

//		storedFileDownloader.cancel();
	}

	public Observable<StoredFileJobStatus> observeLibrarySync(Library library) {
		serviceFilesToSyncCollector
			.streamServiceFilesToSync()
			.
		final Promise<Collection<ServiceFile>> promisedServiceFilesToSync = serviceFilesToSyncCollector.streamServiceFilesToSync();
		cancellationProxy.doCancel(promisedServiceFilesToSync);

		return Observable.create(emitter -> promisedServiceFilesToSync
			.eventually(allServiceFilesToSync -> {
				final HashSet<ServiceFile> serviceFilesSet = allServiceFilesToSync instanceof HashSet ? (HashSet<ServiceFile>)allServiceFilesToSync : new HashSet<>(allServiceFilesToSync);
				final Promise<Void> pruneFilesTask = storedFileAccess.pruneStoredFiles(library, serviceFilesSet);
				pruneFilesTask.excuse(new VoidResponse<>(e -> logger.warn("There was an error pruning the files", e)));

				return !cancellationProxy.isCancelled()
					? pruneFilesTask.then(voids -> serviceFilesSet)
					: new Promise<Set<ServiceFile>>(Collections.emptySet());
			})
			.eventually(allServiceFilesToSync -> {
				if (cancellationProxy.isCancelled())
					return new Promise<>(Collections.emptySet());

				return Promise.whenAll(Stream.of(allServiceFilesToSync)
					.map(serviceFile -> {
						if (cancellationProxy.isCancelled())
							return new Promise<>((StoredFileJob) null);

						final Promise<StoredFileJob> promiseDownloadedStoredFile = storedFileUpdater
							.promiseStoredFileUpdate(library, serviceFile)
							.then(storedFile -> {
								if (storedFile != null && !storedFile.isDownloadComplete())
									return new StoredFileJob(serviceFile, storedFile);

								return null;
							});

						promiseDownloadedStoredFile
							.excuse(r -> {
								logger.warn("An error occurred creating or updating " + serviceFile, r);
								return null;
							});

						return promiseDownloadedStoredFile;
					})
					.toList());
			})
			.then(storedFiles -> {
				if (!cancellationProxy.isCancelled())
					storedFileDownloader
						.process(new ArrayDeque<>(Stream.of(vs).filter(s -> s != null).toList()))
						.subscribe(new Observer<StoredFileJobStatus>() {
							@Override
							public void onSubscribe(Disposable d) {
								emitter.setDisposable(d);
							}

							@Override
							public void onNext(StoredFileJobStatus status) {
								emitter.onNext(status);
							}

							@Override
							public void onError(Throwable e) {
								emitter.onError(e);
							}

							@Override
							public void onComplete() {
								emitter.onComplete();
							}
						});
				else
					emitter.onComplete();

				return null;
			})
			.excuse(e -> {
				logger.warn("There was an error retrieving the files", e);

				emitter.onComplete();

				return null;
			}));
	}
}
