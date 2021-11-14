package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.CheckForSync
import com.lasthopesoftware.bluewater.client.stored.library.sync.ControlLibrarySyncs
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages
import com.lasthopesoftware.bluewater.shared.observables.stream
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Completable
import io.reactivex.exceptions.CompositeException
import org.slf4j.LoggerFactory

class StoredFileSynchronization(
	private val libraryProvider: ILibraryProvider,
	private val messenger: SendMessages,
	private val pruneStoredFiles: PruneStoredFiles,
	private val checkSync: CheckForSync,
	private val syncHandler: ControlLibrarySyncs) : SynchronizeStoredFiles {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(StoredFileSynchronization::class.java) }

		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(StoredFileSynchronization::class.java) }
		val onSyncStartEvent by lazy { magicPropertyBuilder.buildProperty("onSyncStartEvent") }
		val onSyncStopEvent by lazy { magicPropertyBuilder.buildProperty("onSyncStopEvent") }
		val onFileQueuedEvent by lazy { magicPropertyBuilder.buildProperty("onFileQueuedEvent") }
		val onFileDownloadingEvent by lazy { magicPropertyBuilder.buildProperty("onFileDownloadingEvent") }
		val onFileDownloadedEvent by lazy { magicPropertyBuilder.buildProperty("onFileDownloadedEvent") }
		val onFileWriteErrorEvent by lazy { magicPropertyBuilder.buildProperty("onFileWriteErrorEvent") }
		val onFileReadErrorEvent by lazy { magicPropertyBuilder.buildProperty("onFileReadErrorEvent") }
		val storedFileEventKey by lazy { magicPropertyBuilder.buildProperty("storedFileEventKey") }
	}

	override fun streamFileSynchronization(): Completable {
		logger.info("Starting sync.")
		messenger.sendBroadcast(Intent(onSyncStartEvent))
		return CancellableProxyPromise { cp ->
				pruneStoredFiles
					.pruneDanglingFiles()
					.also(cp::doCancel)
					.eventually { checkSync.promiseIsSyncNeeded() }
					.eventually { isNeeded ->
						if (isNeeded) libraryProvider.allLibraries
						else Promise(emptyList())
					}
			}
			.stream()
			.flatMap({ library -> syncHandler.observeLibrarySync(library.libraryId) }, true)
			.flatMapCompletable({ storedFileJobStatus ->
				when (storedFileJobStatus.storedFileJobState) {
					StoredFileJobState.Queued -> {
						sendStoredFileBroadcast(onFileQueuedEvent, storedFileJobStatus.storedFile)
						Completable.complete()
					}
					StoredFileJobState.Downloading -> {
						sendStoredFileBroadcast(onFileDownloadingEvent, storedFileJobStatus.storedFile)
						Completable.complete()
					}
					StoredFileJobState.Downloaded -> {
						sendStoredFileBroadcast(onFileDownloadedEvent, storedFileJobStatus.storedFile)
						Completable.complete()
					}
					else ->	Completable.complete()
				}
			}, true)
			.onErrorComplete(::handleError)
			.doOnComplete(::sendStoppedSync)
			.doOnDispose(::sendStoppedSync)
	}

	private fun handleError(e: Throwable): Boolean {
		return when (e) {
			is CompositeException -> e.exceptions.all { handleError(it) }
			is StoredFileWriteException -> {
				sendStoredFileBroadcast(onFileWriteErrorEvent, e.storedFile)
				return true
			}
			is StoredFileReadException -> {
				sendStoredFileBroadcast(onFileReadErrorEvent, e.storedFile)
				return true
			}
			is StorageCreatePathException -> true
			is StoredFileJobException -> true
			else -> false
		}
	}

	private fun sendStoppedSync() {
		messenger.sendBroadcast(Intent(onSyncStopEvent))
	}

	private fun sendStoredFileBroadcast(action: String, storedFile: StoredFile) {
		val storedFileBroadcastIntent = Intent(action)
		storedFileBroadcastIntent.putExtra(storedFileEventKey, storedFile.id)
		messenger.sendBroadcast(storedFileBroadcastIntent)
	}
}
