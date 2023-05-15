package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.BroadcastReadPermissionsRequest
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.cls
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class StoredFileReadPermissionsReceiver(
	private val readPermissionArbitratorForOs: CheckOsPermissions,
	private val readPermissionsRequestedBroadcast: BroadcastReadPermissionsRequest,
	private val storedFileAccess: AccessStoredFiles
) : ReceiveStoredFileEvent, ImmediateResponse<StoredFile?, Unit> {
	override fun receive(storedFileId: Int): Promise<Unit> =
		if (!readPermissionArbitratorForOs.isReadPermissionGranted) storedFileAccess.getStoredFile(storedFileId).then(this)
		else Promise.empty()

	override fun acceptedEvents(): Collection<Class<out StoredFileMessage>> =
		setOf(cls<StoredFileMessage.FileReadError>())

	override fun respond(storedFile: StoredFile?) {
		storedFile?.apply {
			readPermissionsRequestedBroadcast.sendReadPermissionsRequestedBroadcast(LibraryId(libraryId))
		}
	}
}
