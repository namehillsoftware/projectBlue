package com.lasthopesoftware.bluewater.shared.android.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MessageBus(private val localBroadcastManager: LocalBroadcastManager): SendMessages, RegisterForMessages {

	private val receiverSync = Any()
	private val receivers = HashMap<ReceiveBroadcastEvents, BroadcastReceiver>()

	override fun sendBroadcast(intent: Intent) {
		localBroadcastManager.sendBroadcast(intent)
	}

	override fun registerReceiver(receiver: ReceiveBroadcastEvents, filter: IntentFilter) {
		synchronized(receiverSync) {
			if (receivers.containsKey(receiver)) return

			val delegatedReceiver = DelegatedBroadcastReceiver(receiver)
			receivers[receiver] = delegatedReceiver
			localBroadcastManager.registerReceiver(delegatedReceiver, filter)
		}
	}

	override fun unregisterReceiver(receiver: ReceiveBroadcastEvents) {
		synchronized(receiverSync) {
			receivers.remove(receiver)?.also(localBroadcastManager::unregisterReceiver)
		}
	}

	fun clear() {
		synchronized(receiverSync) {
			receivers.values.forEach(localBroadcastManager::unregisterReceiver)
			receivers.clear()
		}
	}

	private class DelegatedBroadcastReceiver(private val broadcastReceiver: ReceiveBroadcastEvents) : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (context != null && intent != null) broadcastReceiver.onReceive(context, intent)
		}
	}
}
