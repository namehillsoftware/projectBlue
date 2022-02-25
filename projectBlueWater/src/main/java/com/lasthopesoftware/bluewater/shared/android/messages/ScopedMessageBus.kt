package com.lasthopesoftware.bluewater.shared.android.messages

import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import java.util.concurrent.ConcurrentHashMap

class ScopedMessageBus(private val contentResolver: ContentResolver, private val handler: Handler) :
	SendMessages,
	RegisterForMessages,
	AutoCloseable
{
	private val subscriptions = ConcurrentHashMap<ReceiveBroadcastEvents, IntentFilter>()

	private var isClosed = false

	override fun sendBroadcast(intent: Intent) {
		if (isClosed) return

		if (Thread.currentThread() == handler.looper.thread) intent.sendToSubscribers()
		else handler.post { intent.sendToSubscribers() }
	}

	override fun registerReceiver(receiver: ReceiveBroadcastEvents, filter: IntentFilter) {
		if (!isClosed) subscriptions.putIfAbsent(receiver, filter)
	}

	override fun unregisterReceiver(receiver: ReceiveBroadcastEvents) {
		subscriptions.remove(receiver)
	}

	override fun close() {
		if (isClosed) return

		isClosed = true
		subscriptions.clear()
	}

	private fun Intent.sendToSubscribers() {
		val type = resolveTypeIfNeeded(contentResolver)
		subscriptions
			.filter { it.value.match(action, type, scheme, data, categories, "ScopedMessageBus") >= 0 }
			.forEach { it.key.onReceive(this) }
	}
}
