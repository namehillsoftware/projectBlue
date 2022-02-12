package com.lasthopesoftware.bluewater.client.stored.sync.receivers

import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents

class SyncStartedReceiver(private val syncNotification: PostSyncNotification) : ReceiveBroadcastEvents {
	override fun onReceive(context: Context, intent: Intent) {
		syncNotification.notify(null)
	}

	fun acceptedEvents(): Collection<String> = setOf(StoredFileSynchronization.onSyncStartEvent)
}
