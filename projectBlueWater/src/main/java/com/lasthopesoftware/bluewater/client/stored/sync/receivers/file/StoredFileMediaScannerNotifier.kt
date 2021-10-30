package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.broadcasts.IScanMediaFileBroadcaster
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.io.File

class StoredFileMediaScannerNotifier(
	private val storedFileAccess: AccessStoredFiles,
	private val mediaFileBroadcaster: IScanMediaFileBroadcaster
) : ReceiveStoredFileEvent, ImmediateResponse<StoredFile?, Unit> {
	override fun receive(storedFileId: Int): Promise<Unit> {
		return storedFileAccess.getStoredFile(storedFileId)
			.then(this)
	}

	override fun acceptedEvents(): Collection<String> {
		return setOf(StoredFileSynchronization.onFileDownloadedEvent)
	}

	override fun respond(storedFile: StoredFile?) {
		storedFile?.apply {
			mediaFileBroadcaster.sendScanMediaFileBroadcastForFile(File(path))
		}
	}
}
