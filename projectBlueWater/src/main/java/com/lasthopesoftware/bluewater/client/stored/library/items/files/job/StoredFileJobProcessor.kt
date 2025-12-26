package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.ProduceStoredFileDestinations
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.DownloadStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.observables.observeProgress
import com.lasthopesoftware.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.eventuallyUse
import com.lasthopesoftware.resources.io.PromisingWritableStream
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Observable
import java.io.IOException
import java.util.concurrent.CancellationException

class StoredFileJobProcessor(
	private val storedFileFileProvider: ProduceStoredFileDestinations,
	private val storedFiles: DownloadStoredFiles,
	private val updateStoredFiles: UpdateStoredFiles,
) : ProcessStoredFileJobs {

	companion object {
		private val logger by lazyLogger<StoredFileJobProcessor>()

		private fun getCancelledStoredFileJobResult(storedFile: StoredFile): StoredFileJobStatus =
			StoredFileJobStatus(storedFile, StoredFileJobState.Cancelled)
	}

	override fun observeStoredFileDownload(jobs: Observable<StoredFileJob>): Observable<StoredFileJobStatus> {
		val rateLimiter = PromisingRateLimiter<Unit>(1)
		return jobs
			.distinct()
			.flatMap { (libraryId, _, storedFile) ->
				Observable
					.just(StoredFileJobStatus(storedFile, StoredFileJobState.Queued))
					.concatWith(rateLimiter.enqueueProgressingPromise { StoredFileDownloadPromise(libraryId, storedFile) }.observeProgress())
			}
	}

	private fun PromisingWritableStream.promiseDownload(libraryId: LibraryId, storedFile: StoredFile): Promise<StoredFileJobStatus> = Promise.Proxy { cp ->
		val promisedDownload = storedFiles
			.promiseDownload(libraryId, storedFile)
			.also(cp::doCancel)

		promisedDownload.eventuallyUse { s ->
			if (cp.isCancelled) getCancelledStoredFileJobResult(storedFile).toPromise()
			else promiseCopyFrom(s)
				.also(cp::doCancel)
				.eventually { downloadedBytes ->
					if (downloadedBytes > 0) updateStoredFiles.markStoredFileAsDownloaded(storedFile)
					else storedFile.toPromise()
				}
				.then { sf ->
					StoredFileJobStatus(
						sf,
						if (sf.isDownloadComplete) StoredFileJobState.Downloaded
						else StoredFileJobState.Queued
					)
				}
		}
	}

	private inner class StoredFileDownloadPromise(
		private val libraryId: LibraryId,
		private val storedFile: StoredFile,
	) : ProgressingPromiseProxy<StoredFileJobStatus, Unit>() {
		init {
			proxy(promiseProgressingDownload())
		}

		fun promiseProgressingDownload(): Promise<Unit> = if (isCancelled) {
			reportProgress(getCancelledStoredFileJobResult(storedFile))
			Unit.toPromise()
		} else storedFileFileProvider
			.promiseOutputStream(storedFile)
			.eventually { outputStream ->
				outputStream
					?.eventuallyUse { writableStream ->
						if (isCancelled) {
							getCancelledStoredFileJobResult(storedFile).toPromise()
						} else {
							reportProgress(StoredFileJobStatus(storedFile, StoredFileJobState.Downloading))

							writableStream.promiseDownload(libraryId, storedFile).also(::doCancel)
						}
					}
					?.then(::reportProgress) { error ->
						val status = when (error) {
							is CancellationException -> getCancelledStoredFileJobResult(storedFile)
							is StoredFileReadException -> StoredFileJobStatus(storedFile, StoredFileJobState.Unreadable)

							is IOException -> {
								logger.error("Error writing file!", error)
								StoredFileJobStatus(storedFile, StoredFileJobState.Queued)
							}

							is StoredFileJobException -> throw error
							else -> throw StoredFileJobException(storedFile, error)
						}

						reportProgress(status)
					}
					?: storedFile.run {
						reportProgress(
							StoredFileJobStatus(
								storedFile,
								if (isDownloadComplete) StoredFileJobState.Downloaded
								else StoredFileJobState.Unreadable
							)
						)

						Unit.toPromise()
					}
			}
	}
}
