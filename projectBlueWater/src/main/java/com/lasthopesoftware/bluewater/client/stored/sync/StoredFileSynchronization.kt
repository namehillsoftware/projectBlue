package com.lasthopesoftware.bluewater.client.stored.sync

import com.lasthopesoftware.bluewater.client.browsing.library.access.ProvideLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.sync.CheckForSync
import com.lasthopesoftware.bluewater.client.stored.library.sync.ControlLibrarySyncs
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.bluewater.shared.observables.stream
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.exceptions.CompositeException

class StoredFileSynchronization(
    private val libraryProvider: ProvideLibraries,
    private val applicationMessages: SendApplicationMessages,
    private val pruneStoredFiles: PruneStoredFiles,
    private val checkSync: CheckForSync,
    private val syncHandler: ControlLibrarySyncs
) : SynchronizeStoredFiles {

	companion object {
		private val logger by lazyLogger<StoredFileSynchronization>()
	}

	override fun streamFileSynchronization(): Completable {
		logger.info("Starting sync.")
		applicationMessages.sendMessage(SyncStateMessage.SyncStarted)
		return Promise.Proxy { cp ->
				pruneStoredFiles
					.pruneDanglingFiles()
					.also(cp::doCancel)
					.eventually { checkSync.promiseIsSyncNeeded() }
					.eventually { isNeeded ->
						if (isNeeded && !cp.isCancelled) libraryProvider.promiseAllLibraries()
						else Promise(emptyList())
					}
			}
			.stream()
			.flatMap({ library -> syncHandler.observeLibrarySync(library.libraryId) }, true)
			.flatMapCompletable({ storedFileJobStatus ->
				val message = when (storedFileJobStatus.storedFileJobState) {
					StoredFileJobState.Queued -> StoredFileMessage.FileQueued(storedFileJobStatus.storedFile.id)
					StoredFileJobState.Downloading -> StoredFileMessage.FileDownloading(storedFileJobStatus.storedFile.id)
					StoredFileJobState.Downloaded -> StoredFileMessage.FileDownloaded(storedFileJobStatus.storedFile.id)
					else ->	null
				}

				message?.let(applicationMessages::sendMessage)
				Completable.complete()
			}, true)
			.onErrorComplete(::handleError)
			.doOnComplete(::sendStoppedSync)
			.doOnDispose(::sendStoppedSync)
	}

	private fun handleError(e: Throwable): Boolean {
		return when (e) {
			is CompositeException -> e.exceptions.all { handleError(it) }
			is StoredFileWriteException -> {
				applicationMessages.sendMessage(StoredFileMessage.FileWriteError(e.storedFile.id))
				return true
			}
			is StoredFileReadException -> {
				applicationMessages.sendMessage(StoredFileMessage.FileReadError(e.storedFile.id))
				return true
			}
			is StorageCreatePathException -> true
			is StoredFileJobException -> true
			else -> false
		}
	}

	private fun sendStoppedSync() {
		applicationMessages.sendMessage(SyncStateMessage.SyncStopped)
	}
}
