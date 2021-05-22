package com.lasthopesoftware.bluewater.shared.android.messages

import android.content.BroadcastReceiver
import android.content.IntentFilter

interface RegisterForMessages {
	fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter)

	fun unregisterReceiver(receiver: BroadcastReceiver)
}
