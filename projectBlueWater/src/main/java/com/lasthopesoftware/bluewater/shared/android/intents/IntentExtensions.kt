package com.lasthopesoftware.bluewater.shared.android.intents

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import com.lasthopesoftware.bluewater.shared.cls

inline fun <reified T: Context> Context.getIntent() = Intent(this, cls<T>())

inline fun <reified T : Parcelable> Intent.safelyGetParcelableExtra(name: String) =
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getParcelableExtra(name, cls<T>())
	} else {
		@Suppress("DEPRECATION")
		getParcelableExtra(name)
	}

fun Int.makePendingIntentImmutable(): Int =
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this or PendingIntent.FLAG_IMMUTABLE
	else this
