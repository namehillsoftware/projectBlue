package com.lasthopesoftware.bluewater.shared.android.intents

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import com.lasthopesoftware.bluewater.shared.cls

inline fun <reified T: Activity> Context.getIntent() = Intent(this, cls<T>())

inline fun <reified T : Parcelable> Intent.safelyGetParcelableExtra(name: String) =
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getParcelableExtra(name, cls<T>())
	} else {
		@Suppress("DEPRECATION")
		getParcelableExtra(name)
	}
