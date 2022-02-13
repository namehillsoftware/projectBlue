package com.lasthopesoftware.bluewater.shared.android.messages

import android.content.IntentFilter

interface RegisterForMessages {
	fun registerReceiver(receiver: ReceiveBroadcastEvents, filter: IntentFilter)

	fun unregisterReceiver(receiver: ReceiveBroadcastEvents)
}
