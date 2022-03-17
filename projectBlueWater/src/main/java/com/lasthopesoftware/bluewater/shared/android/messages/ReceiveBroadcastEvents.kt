package com.lasthopesoftware.bluewater.shared.android.messages

import android.content.Intent

fun interface ReceiveBroadcastEvents {
	fun onReceive(intent: Intent)
}
