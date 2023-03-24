package com.lasthopesoftware.bluewater.settings.hidden

import android.content.Intent
import com.lasthopesoftware.resources.intents.IIntentFactory

class HiddenSettingsActivityIntentBuilder(private val intentFactory: IIntentFactory) {
    fun buildHiddenSettingsIntent(): Intent = intentFactory.getIntent(HiddenSettingsActivity::class.java)
}
