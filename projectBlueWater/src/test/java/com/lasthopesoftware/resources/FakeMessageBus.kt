package com.lasthopesoftware.resources

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

class FakeMessageBus(private val context: Context) : SendMessages, RegisterForMessages {

	private val _recordedIntents: MutableList<Intent> = ArrayList()
	private val receivers: MutableList<Pair<BroadcastReceiver, IntentFilter>> = ArrayList()

	override fun sendBroadcast(intent: Intent) {
		_recordedIntents.add(intent)

		val action = intent.action
		val type = intent.resolveTypeIfNeeded(context.contentResolver)
		val data = intent.data
		val scheme = intent.scheme
		val categories = intent.categories
		for (receiver in receivers.filter { p -> p.second.match(action, type, scheme, data, categories, "FakeMessageSender") >= 0 }) {
			receiver.first.onReceive(context, intent)
		}
	}

	val recordedIntents: List<Intent>
		get() = _recordedIntents

	override fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
		receivers.add(Pair(receiver, filter))
	}

	override fun unregisterReceiver(receiver: BroadcastReceiver) {
		receivers.removeIf { it.first == receiver }
	}
}
