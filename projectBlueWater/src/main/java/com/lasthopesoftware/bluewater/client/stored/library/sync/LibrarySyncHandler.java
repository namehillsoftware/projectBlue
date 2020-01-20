package com.lasthopesoftware.bluewater.client.stored.library.sync;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles;
import com.lasthopesoftware.bluewater.shared.observables.ObservedPromise;
import com.lasthopesoftware.bluewater.shared.observables.StreamedPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.propagation.RejectionProxy;
import com.namehillsoftware.handoff.promises.propagation.ResolutionProxy;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;

import io.reactivex.Observable;

public class LibrarySyncHandler {

	private static final Logger logger = LoggerFactory.getLogger(LibrarySyncHandler.class);

	private final ProcessStoredFileJobs storedFileJobsProcessor;
	private final CollectServiceFilesForSync serviceFilesToSyncCollector;
	private final IStoredFileAccess storedFileAccess;
	private final UpdateStoredFiles storedFileUpdater;

	public LibrarySyncHandler(
		CollectServiceFilesForSync serviceFilesToSyncCollector,
		IStoredFileAccess storedFileAccess,
		UpdateStoredFiles storedFileUpdater,
		ProcessStoredFileJobs storedFileJobsProcessor) {
		this.serviceFilesToSyncCollector = serviceFilesToSyncCollector;
		this.storedFileAccess = storedFileAccess;
		this.storedFileUpdater = storedFileUpdater;
		this.storedFileJobsProcessor = storedFileJobsProcessor;
	}

	public Observable<StoredFileJobStatus> observeLibrarySync(LibraryId libraryId) {

		final Promise<Collection<ServiceFile>> promisedServiceFilesToSync = serviceFilesToSyncCollector.promiseServiceFilesToSync(libraryId);

		return StreamedPromise.stream(new Promise<HashSet<ServiceFile>>(messenger -> {
			final CancellationProxy cancellationProxy = new CancellationProxy();
			messenger.cancellationRequested(cancellationProxy);
			cancellationProxy.doCancel(promisedServiceFilesToSync);
			promisedServiceFilesToSync
				.eventually(allServiceFilesToSync -> {
					final HashSet<ServiceFile> serviceFilesSet = allServiceFilesToSync instanceof HashSet ? (HashSet<ServiceFile>)allServiceFilesToSync : new HashSet<>(allServiceFilesToSync);
					final Promise<Void> pruneFilesTask = storedFileAccess.pruneStoredFiles(libraryId, serviceFilesSet);
					cancellationProxy.doCancel(pruneFilesTask);
					pruneFilesTask.excuse(new VoidResponse<>(e -> logger.warn("There was an error pruning the files", e)));

					return pruneFilesTask.then(voids -> serviceFilesSet);
				})
				.then(new ResolutionProxy<>(messenger), new RejectionProxy(messenger));
			}))
			.map(serviceFile -> {
				final Promise<StoredFileJob> promiseDownloadedStoredFile = storedFileUpdater
					.promiseStoredFileUpdate(libraryId, serviceFile)
					.then(storedFile -> {
						if (storedFile == null || storedFile.isDownloadComplete())
							return null;

						return new StoredFileJob(libraryId, serviceFile, storedFile);
					});

				promiseDownloadedStoredFile
					.excuse(r -> {
						logger.warn("An error occurred creating or updating " + serviceFile, r);
						return null;
					});

				return promiseDownloadedStoredFile;
			})
			.toList()
			.toObservable()
			.flatMap(promises -> {
				final Promise<Observable<StoredFileJobStatus>> observablePromise = Promise.whenAll(promises)
					.then(storedFileJobs -> storedFileJobsProcessor.observeStoredFileDownload(
						Stream.of(storedFileJobs).filter(j -> j != null).toList()));

				return ObservedPromise.observe(observablePromise);
			})
			.flatMap(o -> o);
	}
}
