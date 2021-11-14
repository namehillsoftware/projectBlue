package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.io.IFileStreamWriter
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileSystemFileProducer
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.DownloadStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*

class StoredFileJobProcessor(
	private val storedFileFileProvider: IStoredFileSystemFileProducer,
	private val storedFileAccess: AccessStoredFiles,
	private val storedFiles: DownloadStoredFiles,
	private val fileReadPossibleArbitrator: IFileReadPossibleArbitrator,
	private val fileWritePossibleArbitrator: IFileWritePossibleArbitrator,
	private val fileStreamWriter: IFileStreamWriter
) : ProcessStoredFileJobs
{
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
				val file = storedFileFileProvider.getFile(storedFile)
				observer.onNext(StoredFileJobStatus(file, storedFile, StoredFileJobState.Queued))
				jobsQueue.offer(job)
			}

			this.observer = observer
			processQueue().then(
				{ observer.onComplete() },
				observer::onError)
		}

		private fun processQueue(): Promise<Void> {
			if (cancellationProxy.isCancelled) return Promise.empty()

			val (libraryId, _, storedFile) = jobsQueue.poll() ?: return Promise.empty()

			val file = storedFileFileProvider.getFile(storedFile)
			if (file.exists()) {
				if (!fileReadPossibleArbitrator.isFileReadPossible(file)) {
					observer.onNext(StoredFileJobStatus(file, storedFile, StoredFileJobState.Unreadable))
					return processQueue()
				}

				if (storedFile.isDownloadComplete) {
					observer.onNext(StoredFileJobStatus(file, storedFile, StoredFileJobState.Downloaded))
					return processQueue()
				}
			}

			if (!fileWritePossibleArbitrator.isFileWritePossible(file)) {
				observer.onError(StoredFileWriteException(file, storedFile))
				return Promise.empty()
			}

			val parent = file.parentFile
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
				observer.onError(StorageCreatePathException(parent))
				return Promise.empty()
			}

			if (cancellationProxy.isCancelled) {
				observer.onNext(getCancelledStoredFileJobResult(file, storedFile))
				return Promise.empty()
			}

			observer.onNext(StoredFileJobStatus(file, storedFile, StoredFileJobState.Downloading))
			return storedFiles.promiseDownload(libraryId, storedFile)
				.also(cancellationProxy::doCancel)
				.then(
				{ inputStream ->
						try {
							if (cancellationProxy.isCancelled) getCancelledStoredFileJobResult(file, storedFile)
							else inputStream.use { s ->
								fileStreamWriter.writeStreamToFile(s, file)
								storedFileAccess.markStoredFileAsDownloaded(storedFile)
								StoredFileJobStatus(file, storedFile, StoredFileJobState.Downloaded)
							}
						} catch (ioe: IOException) {
							logger.error("Error writing file!", ioe)
							StoredFileJobStatus(file, storedFile, StoredFileJobState.Queued)
						} catch (t: Throwable) {
							throw StoredFileJobException(storedFile, t)
						}
					},
					{ error ->
						when (error) {
							is IOException -> StoredFileJobStatus(file, storedFile, StoredFileJobState.Queued)
							is StoredFileJobException -> throw error
							else -> throw StoredFileJobException(storedFile, error)
						}
					})
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

		private fun getCancelledStoredFileJobResult(file: File, storedFile: StoredFile): StoredFileJobStatus =
			StoredFileJobStatus(file, storedFile, StoredFileJobState.Cancelled)
	}
}
