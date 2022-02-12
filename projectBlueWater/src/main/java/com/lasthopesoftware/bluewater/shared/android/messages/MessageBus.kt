package com.lasthopesoftware.bluewater.shared.android.messages

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MessageBus(private val localBroadcastManager: LocalBroadcastManager): SendMessages, RegisterForMessages {

	private val receiverSync = Any()
	private val receivers = HashSet<BroadcastReceiver>()

	override fun sendBroadcast(intent: Intent) {
		localBroadcastManager.sendBroadcast(intent)
	}

	override fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
		synchronized(receiverSync) {
			receivers.add(receiver)
			localBroadcastManager.registerReceiver(receiver, filter)
		}
	}

	override fun unregisterReceiver(receiver: BroadcastReceiver) {
		synchronized(receiverSync) {
			receivers.remove(receiver)
			localBroadcastManager.unregisterReceiver(receiver)
		}
	}

	fun clear() {
		synchronized(receiverSync) {
			receivers.forEach(localBroadcastManager::unregisterReceiver)
			receivers.clear()
		}
	}
}
