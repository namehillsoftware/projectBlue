package com.lasthopesoftware.resources

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

class FakeMessageSender : SendMessages, RegisterForMessages {

	private val _recordedIntents: MutableList<Intent> = ArrayList()
	private val receivers: MutableList<Pair<BroadcastReceiver, IntentFilter>> = ArrayList()

	override fun sendBroadcast(intent: Intent) {
		_recordedIntents.add(intent)
		for (receiver in receivers.map { r -> intent. r.second. }) {

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
