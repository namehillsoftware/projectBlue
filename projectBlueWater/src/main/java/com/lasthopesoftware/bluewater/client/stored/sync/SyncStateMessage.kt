package com.lasthopesoftware.bluewater.client.stored.sync

import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

interface SyncStateMessage : ApplicationMessage {
	object SyncStarted : SyncStateMessage
	object SyncStopped : SyncStateMessage
}
