package com.lasthopesoftware.bluewater.shared.android.messages

import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import java.util.concurrent.ConcurrentHashMap

class ScopedMessageBus(private val contentResolver: ContentResolver, private val handler: Handler): SendMessages, RegisterForMessages {

	private val subscriptions = ConcurrentHashMap<ReceiveBroadcastEvents, IntentFilter>()

	override fun sendBroadcast(intent: Intent) {
		if (Thread.currentThread() == handler.looper.thread) {
			with (intent) {
				val type = resolveTypeIfNeeded(contentResolver)
				subscriptions
					.filter { it.value.match(action, type, scheme, data, categories, "ScopedMessageBus") >= 0 }
					.forEach { it.key.onReceive(this) }
			}
			return
		}

		handler.post {
			with (intent) {
				val type = resolveTypeIfNeeded(contentResolver)
				subscriptions
					.filter { it.value.match(action, type, scheme, data, categories, "ScopedMessageBus") >= 0 }
					.forEach { it.key.onReceive(this) }
			}
		}
	}

	override fun registerReceiver(receiver: ReceiveBroadcastEvents, filter: IntentFilter) {
		subscriptions.putIfAbsent(receiver, filter)
	}

	override fun unregisterReceiver(receiver: ReceiveBroadcastEvents) {
		subscriptions.remove(receiver)
	}
}
