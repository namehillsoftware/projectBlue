package com.lasthopesoftware.bluewater.client.stored.sync

import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

interface SyncStateMessage {
	object SyncStarted : ApplicationMessage
	object SyncStopped : ApplicationMessage
}
