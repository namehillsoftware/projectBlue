package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.ScheduleSyncs
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.client.stored.sync.SyncStateMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.observables.LiftedInteractionState
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.observables.mapNotNull
import com.lasthopesoftware.promises.extensions.regardless
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import com.namehillsoftware.handoff.promises.Promise
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicReference

class StoredFilesViewModel(
	private val storedFileAccess: AccessStoredFiles,
	applicationMessages: RegisterForApplicationMessages,
	private val scheduler: ScheduleSyncs,
) : ViewModel(), TrackLoadedViewState {

	private val autoCloseableManager = AutoCloseableManager().also(::addCloseable)

	@Volatile
	private var isPartiallyUpdating = false
	private val lastPromisedFileUpdate = AtomicReference(Unit.toPromise())

	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableIsSyncing = MutableInteractionState(false)
	private val mutableIsSyncStateChangeEnabled = MutableInteractionState(false)
	private val mutableSyncingFilesWithState = MutableInteractionState(emptyMap<Int, Pair<StoredFile, StoredFileJobState>>())
	private val mutableActiveLibraryId = MutableInteractionState<LibraryId?>(null)

	val activeLibraryId = mutableActiveLibraryId.asInteractionState()

	val isSyncing = mutableIsSyncing.asInteractionState()
	val isSyncStateChangeEnabled = mutableIsSyncStateChangeEnabled.asInteractionState()

	val syncingFiles by lazy {
		autoCloseableManager.manage(
			LiftedInteractionState(
				mutableSyncingFilesWithState
					.mapNotNull()
					.filter { !isPartiallyUpdating }
					.map { m ->
						val downloadingArray = ArrayList<Pair<StoredFile, StoredFileJobState>>()
						val returnList = LinkedList<Pair<StoredFile, StoredFileJobState>>()
						val addedFiles = HashSet<Int>(m.values.size)

						for ((k, p) in m) {
							val (f, s) = p
							if (f.isDownloadComplete || s == StoredFileJobState.Downloaded) continue
							if (!addedFiles.add(f.id)) continue
							if (s == StoredFileJobState.Downloading) downloadingArray.add(p)
							else returnList.add(p)
						}

						returnList.addAll(0, downloadingArray)
						returnList
					},
				mutableSyncingFilesWithState
					.value
					.let { m ->
						val downloadingArray = ArrayList<Pair<StoredFile, StoredFileJobState>>()
						val returnList = LinkedList<Pair<StoredFile, StoredFileJobState>>()
						val addedFiles = HashSet<Int>(m.values.size)

						for ((k, p) in m) {
							val (f, s) = p
							if (f.isDownloadComplete || s == StoredFileJobState.Downloaded) continue
							if (!addedFiles.add(f.id)) continue
							if (s == StoredFileJobState.Downloading) downloadingArray.add(p)
							else returnList.add(p)
						}

						returnList.addAll(0, downloadingArray)
						returnList
					}
			)
		)
	}

	val syncedFiles by lazy {
		autoCloseableManager.manage(
			LiftedInteractionState(
				mutableSyncingFilesWithState
					.mapNotNull()
					.filter { !isPartiallyUpdating }
					.map { m ->
						m.values
							.asSequence()
							.filter { (f, s) -> f.isDownloadComplete || s == StoredFileJobState.Downloaded }
							.map { (f, s) -> f }
							.toList()
					},
				mutableSyncingFilesWithState.value.values
					.asSequence()
					.filter { (f, s) -> f.isDownloadComplete || s == StoredFileJobState.Downloaded }
					.map { (f, s) -> f }
					.toList()
			)
		)
	}

	val allFilesCount by lazy {
		autoCloseableManager.manage(
			LiftedInteractionState(
				mutableSyncingFilesWithState
					.mapNotNull()
					.filter { !isPartiallyUpdating }
					.map { m -> m.size },
				mutableSyncingFilesWithState.value.size
			)
		)
	}

	override val isLoading = mutableIsLoading.asInteractionState()

	init {
		addCloseable(applicationMessages.registerReceiver { message: StoredFileMessage.FileDownloaded ->
			updateStoredFileState(message.storedFileId, StoredFileJobState.Downloaded)
		})

		addCloseable(applicationMessages.registerReceiver { message: StoredFileMessage.FileQueued ->
			updateStoredFileState(message.storedFileId, StoredFileJobState.Queued)
		})

		addCloseable(applicationMessages.registerReceiver { message: StoredFileMessage.FileDownloading ->
			updateStoredFileState(message.storedFileId, StoredFileJobState.Downloading)
		})

		addCloseable(applicationMessages.registerReceiver { message: StoredFileMessage.FileWriteError ->
			updateStoredFileState(message.storedFileId, StoredFileJobState.Queued)
		})

		addCloseable(applicationMessages.registerReceiver { message: StoredFileMessage.FileReadError ->
			updateStoredFileState(message.storedFileId, StoredFileJobState.Queued)
		})

		addCloseable(applicationMessages.registerReceiver { _ : SyncStateMessage.SyncStarted ->
			mutableIsSyncing.value = true
		})

		addCloseable(applicationMessages.registerReceiver { _ : SyncStateMessage.SyncStopped ->
			mutableIsSyncing.value = false
		})

	    scheduler
			.promiseIsSyncing()
			.then {
				mutableIsSyncing.value = it
				mutableIsSyncStateChangeEnabled.value = true
			}
	}

	fun loadActiveDownloads(libraryId: LibraryId? = null): Promise<*> {
		mutableIsLoading.value = true
		mutableActiveLibraryId.value = libraryId

		return storedFileAccess
			.promiseAllStoredFiles(libraryId)
			.then { storedFiles ->
				val filteredStoredFiles =
					libraryId
						?.run {
							storedFiles.filter { sf -> sf.libraryId == id }
						}
						?: storedFiles

				mutableSyncingFilesWithState.value = filteredStoredFiles
					.associate { sf -> sf.id to (sf to if (!sf.isDownloadComplete) StoredFileJobState.Queued else StoredFileJobState.Downloaded) }
			}
			.must { _ -> mutableIsLoading.value = false }
	}

	fun toggleSync() {
		if (!mutableIsSyncStateChangeEnabled.value) return

		mutableIsSyncStateChangeEnabled.value = false
		scheduler
			.promiseIsSyncing()
			.eventually { isSyncRunning ->
				if (isSyncRunning) scheduler.cancelSync()
				else scheduler.syncImmediately()
			}
			.must { _ ->
				mutableIsSyncStateChangeEnabled.value = true
			}
	}

	private fun updateStoredFileState(storedFileId: Int, state: StoredFileJobState) {
		lastPromisedFileUpdate.getAndUpdate { prior ->
			prior.regardless {
				mutableSyncingFilesWithState.value[storedFileId]
					?.let { (storedFile, currentState) ->
						if (currentState != state) {
							isPartiallyUpdating = true
							mutableSyncingFilesWithState.value -= storedFile.id
							isPartiallyUpdating = false
							mutableSyncingFilesWithState.value += storedFile.id to (storedFile to state)
						}
					}
					?.toPromise()
					?: storedFileAccess.promiseStoredFile(storedFileId).then { storedFile ->
						if (storedFile != null && storedFile.libraryId == activeLibraryId.value?.id) {
							isPartiallyUpdating = true
							mutableSyncingFilesWithState.value -= storedFile.id
							isPartiallyUpdating = false
							mutableSyncingFilesWithState.value += storedFile.id to (storedFile to state)
						}
					}
			}
		}
	}
}
