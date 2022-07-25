package com.lasthopesoftware.bluewater.client.stored.sync.receivers

import com.lasthopesoftware.bluewater.client.stored.sync.SyncStateMessage
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.PostSyncNotification

class SyncStartedReceiver(private val syncNotification: PostSyncNotification) : (SyncStateMessage.SyncStarted) -> Unit {
	override fun invoke(p1: SyncStateMessage.SyncStarted) {
		syncNotification.notify(null)
	}
}
