package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.bluewater.shared.observables.stream
import com.lasthopesoftware.bluewater.shared.observables.toMaybeObservable
import com.namehillsoftware.handoff.promises.propagation.ProxyPromise
import io.reactivex.rxjava3.core.Observable
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
		ProxyPromise { cancellationProxy ->
			val pruneFilesTasks = storedFilePruner.pruneStoredFiles(libraryId)

			cancellationProxy.doCancel(pruneFilesTasks)

			serviceFilesToSyncCollector
				.promiseServiceFilesToSync(libraryId)
				.eventually { allServiceFilesToSync ->
					val serviceFilesSet = allServiceFilesToSync as? Set<ServiceFile> ?: allServiceFilesToSync.toSet()
					pruneFilesTasks.excuse { e -> logger.warn("There was an error pruning the files", e) }
					pruneFilesTasks.then { _ -> serviceFilesSet }
				}
		}
		.stream()
		.flatMapMaybe { serviceFile ->
			storedFileUpdater
				.promiseStoredFileUpdate(libraryId, serviceFile)
				.then { storedFile ->
					storedFile
						?.takeUnless { sf -> sf.isDownloadComplete }
						?.let { sf -> StoredFileJob(libraryId, serviceFile, sf) }
				}
				.toMaybeObservable()
		}
		.toList()
		.flatMapObservable(storedFileJobsProcessor::observeStoredFileDownload)
}
