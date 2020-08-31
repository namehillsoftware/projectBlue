package com.lasthopesoftware.bluewater.shared.android

import android.content.Context
import android.content.Intent

fun interface ReceiveBroadcastEvents {
	fun onReceive(context: Context, intent: Intent)
}
