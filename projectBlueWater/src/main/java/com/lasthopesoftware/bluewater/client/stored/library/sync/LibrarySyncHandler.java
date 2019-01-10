package com.lasthopesoftware.bluewater.client.stored.library.sync;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles;
import com.lasthopesoftware.bluewater.shared.observables.ObservedPromise;
import com.lasthopesoftware.bluewater.shared.observables.StreamedPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.vedsoft.futures.runnables.OneParameterAction;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;

public class LibrarySyncHandler {

	private static final Logger logger = LoggerFactory.getLogger(LibrarySyncHandler.class);

	private final ProcessStoredFileJobs storedFileJobsProcessor;
	private final Library library;
	private final CollectServiceFilesForSync serviceFilesToSyncCollector;
	private final IStoredFileAccess storedFileAccess;
	private final UpdateStoredFiles storedFileUpdater;

	private final CancellationProxy cancellationProxy = new CancellationProxy();
	private OneParameterAction<StoredFile> onFileQueued;

	public LibrarySyncHandler(
		Library library,
		CollectServiceFilesForSync serviceFilesToSyncCollector,
		IStoredFileAccess storedFileAccess,
		UpdateStoredFiles storedFileUpdater,
		ProcessStoredFileJobs storedFileJobsProcessor,
		ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider,
		ILibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider) {
		this.library = library;
		this.serviceFilesToSyncCollector = serviceFilesToSyncCollector;
		this.storedFileAccess = storedFileAccess;
		this.storedFileUpdater = storedFileUpdater;
		this.storedFileJobsProcessor = storedFileJobsProcessor;
	}

	public void setOnFileQueued(OneParameterAction<StoredFile> onFileQueued) {
		this.onFileQueued = onFileQueued;
	}

	//	public void setOnFileReadError(OneParameterAction<StoredFile> onFileReadError) {
//		storedFileDownloader.setOnFileReadError(storedFile -> {
//			if (libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(library))
//				onFileReadError.runWith(library, storedFile);
//		});
//	}

//	public void setOnFileWriteError(OneParameterAction<StoredFile> onFileWriteError) {
//		storedFileDownloader.setOnFileWriteError(storedFile -> {
//			if (libraryStorageWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(library))
//				onFileWriteError.runWith(library, storedFile);
//		});
//	}

	public void cancel() {
		cancellationProxy.run();

//		storedFileDownloader.cancel();
	}

	public Observable<StoredFileJobStatus> observeLibrarySync() {

		final Promise<Collection<ServiceFile>> promisedServiceFilesToSync = serviceFilesToSyncCollector.promiseServiceFilesToSync();

		return StreamedPromise.stream(promisedServiceFilesToSync
			.eventually(allServiceFilesToSync -> {
				final HashSet<ServiceFile> serviceFilesSet = allServiceFilesToSync instanceof HashSet ? (HashSet<ServiceFile>)allServiceFilesToSync : new HashSet<>(allServiceFilesToSync);
				final Promise<Void> pruneFilesTask = storedFileAccess.pruneStoredFiles(library, serviceFilesSet);
				pruneFilesTask.excuse(new VoidResponse<>(e -> logger.warn("There was an error pruning the files", e)));

				return pruneFilesTask.then(voids -> serviceFilesSet);
			}))
			.flatMap(serviceFile -> {
				final Promise<Observable<StoredFileJobStatus>> promiseDownloadedStoredFile = storedFileUpdater
					.promiseStoredFileUpdate(library, serviceFile)
					.then(storedFile -> {
						if (storedFile == null || storedFile.isDownloadComplete())
							return Observable.empty();

						final Observable<StoredFileJobStatus> observeStoredFileDownload = this.storedFileJobsProcessor.observeStoredFileDownload(new StoredFileJob(serviceFile, storedFile));

						if (onFileQueued != null)
							onFileQueued.runWith(storedFile);

						return observeStoredFileDownload;
					});

				promiseDownloadedStoredFile
					.excuse(r -> {
						logger.warn("An error occurred creating or updating " + serviceFile, r);
						return null;
					});

				return ObservedPromise.observe(promiseDownloadedStoredFile);
			})
			.flatMap(o -> o, true);
	}
}
