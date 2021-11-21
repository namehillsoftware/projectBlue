package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import com.lasthopesoftware.bluewater.client.browsing.library.request.read.IStorageReadPermissionsRequestedBroadcast
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class StoredFileReadPermissionsReceiver(
	private val readPermissionArbitratorForOs: IStorageReadPermissionArbitratorForOs,
	private val readPermissionsRequestedBroadcast: IStorageReadPermissionsRequestedBroadcast,
	private val storedFileAccess: AccessStoredFiles
) : ReceiveStoredFileEvent, ImmediateResponse<StoredFile?, Unit> {
	override fun receive(storedFileId: Int): Promise<Unit> =
		if (!readPermissionArbitratorForOs.isReadPermissionGranted) storedFileAccess.getStoredFile(storedFileId).then(this)
		else Promise.empty()

	override fun acceptedEvents(): Collection<String> {
		return setOf(StoredFileSynchronization.onFileReadErrorEvent)
	}

	override fun respond(storedFile: StoredFile?) {
		storedFile?.apply {
			readPermissionsRequestedBroadcast.sendReadPermissionsRequestedBroadcast(libraryId)
		}
	}
}
