package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.ProduceStoredFileDestinations
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.DownloadStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.useEventually
import com.lasthopesoftware.resources.io.PromisingOutputStreamWrapper
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.io.IOException
import java.util.LinkedList
import java.util.concurrent.CancellationException

class StoredFileJobProcessor(
	private val storedFileFileProvider: ProduceStoredFileDestinations,
	private val storedFileAccess: AccessStoredFiles,
	private val storedFiles: DownloadStoredFiles
) : ProcessStoredFileJobs {

	override fun observeStoredFileDownload(jobs: Iterable<StoredFileJob>): Observable<StoredFileJobStatus> =
		RecursiveQueueProcessor(jobs)

	private inner class RecursiveQueueProcessor(private val jobs: Iterable<StoredFileJob>) :
		Observable<StoredFileJobStatus>(),
		PromisedResponse<Unit, Unit>,
		Disposable
	{
		private val cancellationProxy = CancellationProxy()
		private val jobsQueue = LinkedList<StoredFileJob>()
		private lateinit var observer: Observer<in StoredFileJobStatus>
		private var isRunning = false

		@Synchronized
		override fun subscribeActual(observer: Observer<in StoredFileJobStatus>) {
			if (isRunning) {
				observer.onComplete()
				return
			}
			isRunning = true

			observer.onSubscribe(this)
			for (job in jobs.distinct()) {
				val storedFile = job.storedFile
				observer.onNext(StoredFileJobStatus(storedFile, StoredFileJobState.Queued))
				jobsQueue.offer(job)
			}

			this.observer = observer
			processQueue().then(
				{ observer.onComplete() },
				observer::onError)
		}

		private fun processQueue(): Promise<Unit> {
			if (cancellationProxy.isCancelled) return Unit.toPromise()

			val (libraryId, _, storedFile) = jobsQueue.poll() ?: return Unit.toPromise()

			return storedFileFileProvider
				.promiseOutputStream(storedFile)
				.eventually { outputStream ->
					outputStream
						?.let {
							if (cancellationProxy.isCancelled) {
								it.close()
								getCancelledStoredFileJobResult(storedFile).toPromise()
							} else {
								observer.onNext(StoredFileJobStatus(storedFile, StoredFileJobState.Downloading))
								PromisingOutputStreamWrapper(it)
									.useEventually { outputStreamWrapper ->
										storedFiles.promiseDownload(libraryId, storedFile)
											.also(cancellationProxy::doCancel)
											.eventually { inputStream ->
												if (cancellationProxy.isCancelled) getCancelledStoredFileJobResult(storedFile).toPromise()
												else {
													outputStreamWrapper
														.promiseCopyFrom(inputStream)
														.also(cancellationProxy::doCancel)
														.eventually { storedFileAccess.markStoredFileAsDownloaded(storedFile) }
														.then { sf -> StoredFileJobStatus(sf, StoredFileJobState.Downloaded) }
												}
											}
									}
							}
						}
						?.then(observer::onNext) { error ->
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

							observer.onNext(status)
						}
						?: storedFile.run {
							observer.onNext(
								StoredFileJobStatus(
									storedFile,
									if (isDownloadComplete) StoredFileJobState.Downloaded
									else StoredFileJobState.Unreadable
								)
							)

							Unit.toPromise()
						}
				}
				.eventually(this) { e ->
					observer.onError(e)
					Unit.toPromise()
				}
		}

		override fun promiseResponse(resolution: Unit): Promise<Unit> = processQueue()

		override fun dispose() = cancellationProxy.run()

		override fun isDisposed(): Boolean = cancellationProxy.isCancelled
	}

	companion object {
		private val logger by lazyLogger<StoredFileJobProcessor>()

		private fun getCancelledStoredFileJobResult(storedFile: StoredFile): StoredFileJobStatus =
			StoredFileJobStatus(storedFile, StoredFileJobState.Cancelled)
	}
}
