package com.lasthopesoftware.bluewater.shared.android.intents

import android.app.Activity
import android.content.Context
import android.content.Intent

inline fun <reified T : Activity> Context.startActivity() {
	startActivity(getIntent<T>())
}

inline fun <reified T: Activity> Context.getIntent() = Intent(this, T::class.java)
