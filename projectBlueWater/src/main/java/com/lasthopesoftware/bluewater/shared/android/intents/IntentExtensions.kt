package com.lasthopesoftware.bluewater.shared.android.intents

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.shared.cls

inline fun <reified T: Activity> Context.getIntent() = Intent(this, cls<T>())
