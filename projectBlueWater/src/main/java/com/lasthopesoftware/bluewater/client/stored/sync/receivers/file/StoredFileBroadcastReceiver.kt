package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization

class StoredFileBroadcastReceiver(private val receiveStoredFileEvent: ReceiveStoredFileEvent) :
	BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val storedFileId = intent.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1)
		if (storedFileId > 0) receiveStoredFileEvent.receive(storedFileId)
	}
}
