package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.ScheduleSyncs
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.client.stored.sync.SyncStateMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.observables.LiftedInteractionState
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.observables.mapNotNull
import com.namehillsoftware.handoff.promises.Promise

class ActiveFileDownloadsViewModel(
	private val storedFileAccess: AccessStoredFiles,
	applicationMessages: RegisterForApplicationMessages,
	private val scheduler: ScheduleSyncs,
) : ViewModel(), TrackLoadedViewState {
	private val mutableIsLoading = MutableInteractionState(false)

	private val mutableIsSyncing = MutableInteractionState(false)
	private val mutableIsSyncStateChangeEnabled = MutableInteractionState(false)
	private val mutableDownloadingFiles = MutableInteractionState(emptyMap<Int, StoredFile>())
	private val mutableDownloadingFileId = MutableInteractionState<Int?>(null)
	private val fileDownloadedRegistration = applicationMessages.registerReceiver { message: StoredFileMessage.FileDownloaded ->
		mutableDownloadingFiles.value -= message.storedFileId
	}

	private val fileQueuedRegistration = applicationMessages.registerReceiver { message: StoredFileMessage.FileQueued ->
		message.storedFileId
			.takeUnless(mutableDownloadingFiles.value::containsKey)
			?.let(storedFileAccess::promiseStoredFile)
			?.then { storedFile ->
				if (storedFile != null && storedFile.libraryId == activeLibraryId?.id) {
					mutableDownloadingFiles.value += Pair(storedFile.id, storedFile)
				}
			}
	}

	private val fileDownloadingRegistration = applicationMessages.registerReceiver { message: StoredFileMessage.FileDownloading ->
		mutableDownloadingFileId.value = message.storedFileId
	}

	private val syncStartedReceiver = applicationMessages.registerReceiver { _ : SyncStateMessage.SyncStarted ->
		mutableIsSyncing.value = true
	}

	private val syncStoppedReceiver = applicationMessages.registerReceiver { _ : SyncStateMessage.SyncStopped ->
		mutableIsSyncing.value = false
	}

	var activeLibraryId: LibraryId? = null
		private set

	val isSyncing = mutableIsSyncing.asInteractionState()
	val isSyncStateChangeEnabled = mutableIsSyncStateChangeEnabled.asInteractionState()
	val downloadingFiles = LiftedInteractionState(
		mutableDownloadingFiles.mapNotNull().map { it.values.toList() },
		emptyList()
	)
	val downloadingFileId = mutableDownloadingFileId.asInteractionState()
	override val isLoading = mutableIsLoading.asInteractionState()

	init {
	    scheduler
			.promiseIsSyncing()
			.then { it ->
				mutableIsSyncing.value = it
				mutableIsSyncStateChangeEnabled.value = true
			}
	}

	override fun onCleared() {
		fileDownloadedRegistration.close()
		fileQueuedRegistration.close()
		syncStartedReceiver.close()
		syncStoppedReceiver.close()
		fileDownloadingRegistration.close()
	}

	fun loadActiveDownloads(libraryId: LibraryId): Promise<*> {
		mutableIsLoading.value = true
		activeLibraryId = libraryId
		return storedFileAccess
			.promiseDownloadingFiles()
			.then { storedFiles ->
				mutableDownloadingFiles.value = storedFiles
						.filter { sf -> sf.libraryId == libraryId.id }
						.associateBy { sf -> sf.id }
			}
			.must {  _ -> mutableIsLoading.value = false }
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
}
