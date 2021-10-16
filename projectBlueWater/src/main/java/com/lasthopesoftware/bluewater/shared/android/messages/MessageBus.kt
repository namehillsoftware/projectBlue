package com.lasthopesoftware.bluewater.shared.android.messages

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MessageBus(private val localBroadcastManager: LocalBroadcastManager): SendMessages, RegisterForMessages {

	private val receivers = HashSet<BroadcastReceiver>()

	override fun sendBroadcast(intent: Intent) {
		localBroadcastManager.sendBroadcast(intent)
	}

	override fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
		receivers.add(receiver)
		localBroadcastManager.registerReceiver(receiver, filter)
	}

	override fun unregisterReceiver(receiver: BroadcastReceiver) {
		receivers.remove(receiver)
		localBroadcastManager.unregisterReceiver(receiver)
	}

	fun clear() {
		receivers.forEach(localBroadcastManager::unregisterReceiver)
		receivers.clear()
	}
}
