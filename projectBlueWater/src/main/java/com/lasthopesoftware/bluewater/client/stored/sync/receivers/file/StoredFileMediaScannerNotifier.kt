package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import com.lasthopesoftware.bluewater.client.browsing.files.broadcasts.IScanMediaFileBroadcaster
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.shared.cls
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.io.File

class StoredFileMediaScannerNotifier(
	private val storedFileAccess: AccessStoredFiles,
	private val mediaFileBroadcaster: IScanMediaFileBroadcaster
) : ReceiveStoredFileEvent, ImmediateResponse<StoredFile?, Unit> {
	override fun receive(storedFileId: Int): Promise<Unit> =
		storedFileAccess.getStoredFile(storedFileId).then(this)

	override fun acceptedEvents(): Collection<Class<out StoredFileMessage>> =
		setOf(cls<StoredFileMessage.FileDownloading>())

	override fun respond(storedFile: StoredFile?) {
		storedFile?.path?.also {
			mediaFileBroadcaster.sendScanMediaFileBroadcastForFile(File(it))
		}
	}
}
