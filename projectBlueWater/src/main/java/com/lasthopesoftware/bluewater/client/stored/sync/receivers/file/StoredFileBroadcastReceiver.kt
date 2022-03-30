package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage

class StoredFileBroadcastReceiver(private val receiveStoredFileEvent: ReceiveStoredFileEvent) : (StoredFileMessage) -> Unit {
	override fun invoke(message: StoredFileMessage) {
		receiveStoredFileEvent.receive(message.storedFileId)
	}
}
