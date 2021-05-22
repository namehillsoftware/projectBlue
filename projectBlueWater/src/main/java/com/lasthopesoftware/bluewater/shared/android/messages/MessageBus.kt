package com.lasthopesoftware.bluewater.shared.android.messages

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MessageBus(private val localBroadcastManager: LocalBroadcastManager): SendMessages, RegisterForMessages {

	override fun sendBroadcast(intent: Intent) {
		localBroadcastManager.sendBroadcast(intent)
	}

	override fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
		localBroadcastManager.registerReceiver(receiver, filter)
	}

	override fun unregisterReceiver(receiver: BroadcastReceiver) {
		localBroadcastManager.unregisterReceiver(receiver)
	}
}
