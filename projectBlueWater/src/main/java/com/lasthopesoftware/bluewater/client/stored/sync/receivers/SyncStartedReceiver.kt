package com.lasthopesoftware.bluewater.client.stored.sync.receivers

import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.PostSyncNotification

class SyncStartedReceiver(private val syncNotification: PostSyncNotification) : (StoredFileMessage.SyncStarted) -> Unit {
	override fun invoke(p1: StoredFileMessage.SyncStarted) {
		syncNotification.notify(null)
	}
}
