package com.lasthopesoftware.bluewater.shared.android.intents

import android.content.Context
import android.content.Intent

class IntentFactory(private val context: Context) : IIntentFactory {
    override fun getIntent(cls: Class<*>): Intent = Intent(context, cls)
}
