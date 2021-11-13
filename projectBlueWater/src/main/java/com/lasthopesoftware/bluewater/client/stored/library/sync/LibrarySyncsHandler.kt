package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.bluewater.shared.observables.stream
import com.lasthopesoftware.bluewater.shared.observables.toMaybeObservable
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Observable
import org.slf4j.LoggerFactory

class LibrarySyncsHandler(
	private val serviceFilesToSyncCollector: CollectServiceFilesForSync,
	private val storedFilePruner: PruneStoredFiles,
	private val storedFileUpdater: UpdateStoredFiles,
	private val storedFileJobsProcessor: ProcessStoredFileJobs
) : ControlLibrarySyncs
{

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(LibrarySyncsHandler::class.java) }
	}

	override fun observeLibrarySync(libraryId: LibraryId): Observable<StoredFileJobStatus> =
		CancellableProxyPromise { cancellationProxy ->
			val pruneFilesTasks = Promise.whenAll(
				storedFilePruner.pruneStoredFiles(libraryId),
				storedFilePruner.pruneDanglingFiles())

			cancellationProxy.doCancel(pruneFilesTasks)

			serviceFilesToSyncCollector
				.promiseServiceFilesToSync(libraryId)
				.eventually { allServiceFilesToSync ->
					val serviceFilesSet = allServiceFilesToSync as? Set<ServiceFile> ?: allServiceFilesToSync.toSet()
					pruneFilesTasks.excuse { e -> logger.warn("There was an error pruning the files", e) }
					pruneFilesTasks.then { serviceFilesSet }
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
				.toMaybeObservable()
		}
		.toList()
		.flatMapObservable(storedFileJobsProcessor::observeStoredFileDownload)
}
