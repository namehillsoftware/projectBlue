package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.bluewater.shared.observables.MaybePromise.Companion.toMaybe
import com.lasthopesoftware.bluewater.shared.observables.StreamedPromise.Companion.stream
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import io.reactivex.Observable
import org.slf4j.LoggerFactory

class LibrarySyncsHandler(
	private val serviceFilesToSyncCollector: CollectServiceFilesForSync,
	private val storedFileAccess: IStoredFileAccess,
	private val storedFileUpdater: UpdateStoredFiles,
	private val storedFileJobsProcessor: ProcessStoredFileJobs) : ControlLibrarySyncs {

	override fun observeLibrarySync(libraryId: LibraryId): Observable<StoredFileJobStatus> =
		CancellableProxyPromise { cancellationProxy ->
			serviceFilesToSyncCollector.promiseServiceFilesToSync(libraryId)
				.eventually { allServiceFilesToSync ->
					val serviceFilesSet = allServiceFilesToSync as? Set<ServiceFile> ?: allServiceFilesToSync.toSet()
					val pruneFilesTask = storedFileAccess.pruneStoredFiles(libraryId, serviceFilesSet)
					cancellationProxy.doCancel(pruneFilesTask)
					pruneFilesTask.excuse { e -> logger.warn("There was an error pruning the files", e) }
					pruneFilesTask.then { serviceFilesSet }
				}
		}
		.stream()
		.flatMapMaybe { serviceFile ->
			storedFileUpdater
				.promiseStoredFileUpdate(libraryId, serviceFile)
				.then { storedFile ->
					if (storedFile == null || storedFile.isDownloadComplete) null
					else StoredFileJob(libraryId, serviceFile, storedFile)
				}
				.toMaybe()
		}
		.toList()
		.flatMapObservable { storedFileJobs -> storedFileJobsProcessor.observeStoredFileDownload(storedFileJobs) }

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(LibrarySyncsHandler::class.java) }
	}
}
