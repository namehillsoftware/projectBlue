package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents

class StoredFileBroadcastReceiver(private val receiveStoredFileEvent: ReceiveStoredFileEvent) : ReceiveBroadcastEvents {
	override fun onReceive(context: Context, intent: Intent) {
		val storedFileId = intent.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1)
		if (storedFileId > 0) receiveStoredFileEvent.receive(storedFileId)
	}
}
