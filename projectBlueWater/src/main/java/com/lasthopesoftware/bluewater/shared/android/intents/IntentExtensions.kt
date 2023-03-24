package com.lasthopesoftware.bluewater.shared.android.intents

import android.app.Activity
import android.content.Context
import android.content.Intent

inline fun <reified T : Activity> Context.startActivity() {
	startActivity(Intent(this, T::class.java))
}

inline fun <reified T: Activity> IIntentFactory.getIntent() = getIntent(T::class.java)
