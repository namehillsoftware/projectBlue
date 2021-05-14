package com.lasthopesoftware.bluewater.shared.android.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MessageBus(val context: Context): SendMessages, RegisterForMessages {
	private val localBroadcastManager = lazy { LocalBroadcastManager.getInstance(context) }

	override fun sendBroadcast(intent: Intent) {
		localBroadcastManager.value.sendBroadcast(intent)
	}

	override fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
		localBroadcastManager.value.registerReceiver(receiver, filter)
	}

	override fun unregisterReceiver(receiver: BroadcastReceiver) {
		localBroadcastManager.value.unregisterReceiver(receiver)
	}
}
