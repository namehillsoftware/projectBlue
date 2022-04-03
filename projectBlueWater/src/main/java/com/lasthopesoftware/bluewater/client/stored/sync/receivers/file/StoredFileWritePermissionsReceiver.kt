package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.BroadcastWritePermissionsRequest
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.storage.write.permissions.IStorageWritePermissionArbitratorForOs
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class StoredFileWritePermissionsReceiver(
	private val writePermissionArbitratorForOs: IStorageWritePermissionArbitratorForOs,
	private val writePermissionsRequestedBroadcaster: BroadcastWritePermissionsRequest,
	private val storedFileAccess: AccessStoredFiles
) : ReceiveStoredFileEvent, ImmediateResponse<StoredFile?, Unit> {
	override fun receive(storedFileId: Int): Promise<Unit> =
		if (!writePermissionArbitratorForOs.isWritePermissionGranted) storedFileAccess.getStoredFile(storedFileId).then(this)
		else Promise.empty()

	override fun acceptedEvents(): Collection<Class<out StoredFileMessage>> =
		setOf(cls<StoredFileMessage.FileWriteError>())

	override fun respond(storedFile: StoredFile?) {
		storedFile?.apply {
			writePermissionsRequestedBroadcaster.sendWritePermissionsNeededBroadcast(LibraryId(libraryId))
		}
	}
}
