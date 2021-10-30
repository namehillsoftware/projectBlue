package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import com.lasthopesoftware.bluewater.client.browsing.library.request.write.IStorageWritePermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.storage.write.permissions.IStorageWritePermissionArbitratorForOs
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class StoredFileWritePermissionsReceiver(
	private val writePermissionArbitratorForOs: IStorageWritePermissionArbitratorForOs,
	private val writePermissionsRequestedBroadcaster: IStorageWritePermissionsRequestedBroadcaster,
	private val storedFileAccess: IStoredFileAccess
) : ReceiveStoredFileEvent, ImmediateResponse<StoredFile?, Unit> {
	override fun receive(storedFileId: Int): Promise<Unit> =
		if (!writePermissionArbitratorForOs.isWritePermissionGranted) storedFileAccess.getStoredFile(storedFileId).then(this)
		else Promise.empty()

	override fun acceptedEvents(): Collection<String> {
		return setOf(StoredFileSynchronization.onFileWriteErrorEvent)
	}

	override fun respond(storedFile: StoredFile?) {
		storedFile?.apply {
			writePermissionsRequestedBroadcaster.sendWritePermissionsNeededBroadcast(libraryId)
		}
	}
}
