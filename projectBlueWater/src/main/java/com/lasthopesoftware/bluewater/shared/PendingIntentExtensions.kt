package com.lasthopesoftware.bluewater.shared

import android.app.PendingIntent
import android.os.Build

fun Int.makePendingIntentImmutable(): Int =
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this or PendingIntent.FLAG_IMMUTABLE
	else this
