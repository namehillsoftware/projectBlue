package com.lasthopesoftware.resources

import android.content.Intent
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

class FakeMessageSender : SendMessages {

	private val _recordedIntents: MutableList<Intent> = ArrayList()

	override fun sendBroadcast(intent: Intent) {
		_recordedIntents.add(intent)
	}

	val recordedIntents: List<Intent>
		get() = _recordedIntents
}
