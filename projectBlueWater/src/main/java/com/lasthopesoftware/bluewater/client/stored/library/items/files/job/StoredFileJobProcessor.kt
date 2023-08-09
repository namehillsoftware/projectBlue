package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.ProduceStoredFileDestinations
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.DownloadStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.io.WriteFileStreams
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.LinkedList

class StoredFileJobProcessor(
	private val storedFileFileProvider: ProduceStoredFileDestinations,
	private val storedFileAccess: AccessStoredFiles,
	private val storedFiles: DownloadStoredFiles,
	private val fileReadPossibleArbitrator: IFileReadPossibleArbitrator,
	private val fileWritePossibleArbitrator: IFileWritePossibleArbitrator,
	private val fileStreamWriter: WriteFileStreams
) : ProcessStoredFileJobs {

	override fun observeStoredFileDownload(jobs: Iterable<StoredFileJob>): Observable<StoredFileJobStatus> =
		RecursiveQueueProcessor(jobs)

	private inner class RecursiveQueueProcessor(private val jobs: Iterable<StoredFileJob>) :
		Observable<StoredFileJobStatus>(),
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

			val outputStream = try {
				storedFileFileProvider.getOutputStream(storedFile)
			} catch (e: Exception) {
				observer.onError(e)
				return Unit.toPromise()
			}

			if (outputStream == null) {
				if (storedFile.isDownloadComplete) {
					observer.onNext(StoredFileJobStatus(storedFile, StoredFileJobState.Downloaded))
					return processQueue()
				}

				observer.onNext(StoredFileJobStatus(storedFile, StoredFileJobState.Unreadable))
				return processQueue()
			}

			if (cancellationProxy.isCancelled) {
				observer.onNext(getCancelledStoredFileJobResult(storedFile))
				return Unit.toPromise()
			}

			observer.onNext(StoredFileJobStatus(storedFile, StoredFileJobState.Downloading))
			return storedFiles.promiseDownload(libraryId, storedFile)
				.also(cancellationProxy::doCancel)
				.then(
					{ inputStream ->
						try {
							if (cancellationProxy.isCancelled) getCancelledStoredFileJobResult(storedFile)
							else inputStream.use { s -> s.copyTo(outputStream) }

							storedFileAccess.markStoredFileAsDownloaded(storedFile)
							StoredFileJobStatus(storedFile, StoredFileJobState.Downloaded)
						} catch (sfr: StoredFileReadException) {
							StoredFileJobStatus(storedFile, StoredFileJobState.Unreadable)
						} catch (ioe: IOException) {
							logger.error("Error writing file!", ioe)
							StoredFileJobStatus(storedFile, StoredFileJobState.Queued)
						} catch (t: Throwable) {
							throw StoredFileJobException(storedFile, t)
						}
					},
					{ error ->
						when (error) {
							is IOException -> StoredFileJobStatus(storedFile, StoredFileJobState.Queued)
							is StoredFileJobException -> throw error
							else -> throw StoredFileJobException(storedFile, error)
						}
					})
				.must { outputStream.close() }
				.eventually { status ->
					observer.onNext(status)
					processQueue()
				}
		}

		override fun dispose() = cancellationProxy.run()

		override fun isDisposed(): Boolean = cancellationProxy.isCancelled
	}

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(StoredFileJobProcessor::class.java) }

		private fun getCancelledStoredFileJobResult(storedFile: StoredFile): StoredFileJobStatus =
			StoredFileJobStatus(storedFile, StoredFileJobState.Cancelled)
	}
}
