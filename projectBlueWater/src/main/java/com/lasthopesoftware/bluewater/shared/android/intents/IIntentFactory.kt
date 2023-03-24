package com.lasthopesoftware.bluewater.shared.android.intents

import android.content.Intent

interface IIntentFactory {
    fun getIntent(cls: Class<*>): Intent
}
