package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.bluewater.shared.observables.ObservedPromise
import com.lasthopesoftware.bluewater.shared.observables.StreamedPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Observable
import org.slf4j.LoggerFactory

class LibrarySyncsHandler(
	private val serviceFilesToSyncCollector: CollectServiceFilesForSync,
	private val storedFileAccess: AccessStoredFiles,
	private val storedFileUpdater: UpdateStoredFiles,
	private val storedFileJobsProcessor: ProcessStoredFileJobs) : ControlLibrarySyncs
{

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(LibrarySyncsHandler::class.java) }
	}

	override fun observeLibrarySync(libraryId: LibraryId): Observable<StoredFileJobStatus> {
		val promisedServiceFilesToSync = serviceFilesToSyncCollector.promiseServiceFilesToSync(libraryId)
		return StreamedPromise.stream(CancellableProxyPromise { cancellationProxy ->
			promisedServiceFilesToSync
				.eventually { allServiceFilesToSync ->
					val serviceFilesSet = allServiceFilesToSync as? Set<ServiceFile> ?: allServiceFilesToSync.toSet()
					val pruneFilesTask = storedFileAccess.pruneStoredFiles(libraryId, serviceFilesSet)
					cancellationProxy.doCancel(pruneFilesTask)
					pruneFilesTask.excuse { e -> logger.warn("There was an error pruning the files", e) }
					pruneFilesTask.then { serviceFilesSet }
				}
		})
		.map { serviceFile ->
			storedFileUpdater
				.promiseStoredFileUpdate(libraryId, serviceFile)
				.then { storedFile ->
					if (storedFile == null || storedFile.isDownloadComplete) null
					else StoredFileJob(libraryId, serviceFile, storedFile)
				}
		}
		.toList()
		.toObservable()
		.flatMap { promiseStoredFileJobs ->
			val observablePromise = Promise.whenAll(promiseStoredFileJobs)
				.then { storedFileJobs ->
					storedFileJobsProcessor.observeStoredFileDownload(storedFileJobs.filterNotNull())
				}
			ObservedPromise.observe(observablePromise)
		}
		.flatMap { it }
	}
}
