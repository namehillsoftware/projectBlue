package com.lasthopesoftware.bluewater.handheld

import com.lasthopesoftware.bluewater.ProjectBlueApplication
import com.lasthopesoftware.bluewater.android.intents.newIntentBuilder
import com.lasthopesoftware.bluewater.handheld.client.HandheldEntryActivity

open class HandheldApplication : ProjectBlueApplication() {
	override val intentBuilder by lazy { newIntentBuilder<HandheldEntryActivity>() }
}
