package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.bluewater.shared.observables.stream
import com.lasthopesoftware.bluewater.shared.observables.toMaybeObservable
import com.namehillsoftware.handoff.promises.Promise
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
		private const val IGNORED_ARGUMENT_ERROR = "MIME type application/octet-stream cannot be inserted into content://media/external/audio/media; expected MIME type under audio/*"
		private val logger by lazy { LoggerFactory.getLogger(LibrarySyncsHandler::class.java) }
	}

	override fun observeLibrarySync(libraryId: LibraryId): Observable<StoredFileJobStatus> =
		Promise.Proxy { cancellationProxy ->
			val pruneFilesTasks = storedFilePruner.pruneStoredFiles(libraryId)

			cancellationProxy.doCancel(pruneFilesTasks)

			serviceFilesToSyncCollector
				.promiseServiceFilesToSync(libraryId)
				.eventually { allServiceFilesToSync ->
					pruneFilesTasks.excuse { e -> logger.warn("There was an error pruning the files", e) }
					pruneFilesTasks.then { _ -> allServiceFilesToSync }
				}
		}
		.stream()
		.distinct()
		.flatMapMaybe { serviceFile ->
			storedFileUpdater
				.promiseStoredFileUpdate(libraryId, serviceFile)
				.then<StoredFileJob?>({ storedFile ->
					storedFile
						?.takeUnless { sf -> sf.isDownloadComplete }
						?.let { sf -> StoredFileJob(libraryId, serviceFile, sf) }
				}, { e ->
					if (e !is IllegalArgumentException || e.message != IGNORED_ARGUMENT_ERROR) throw e

					logger.warn("An accepted exception occurred while updating the stored file for $libraryId, $serviceFile, ignoring file.", e)
					null
				})
				.toMaybeObservable()
		}
		.toList()
		.flatMapObservable(storedFileJobsProcessor::observeStoredFileDownload)
}
