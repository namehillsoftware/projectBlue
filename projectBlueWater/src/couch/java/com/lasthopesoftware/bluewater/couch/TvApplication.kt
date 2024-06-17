package com.lasthopesoftware.bluewater.couch

import com.lasthopesoftware.bluewater.ProjectBlueApplication
import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.android.intents.newIntentBuilder
import com.lasthopesoftware.bluewater.client.EntryActivity
import com.lasthopesoftware.bluewater.client.TvEntryActivity
import com.lasthopesoftware.bluewater.shared.cls

open class TvApplication : ProjectBlueApplication() {
	override val intentBuilder by lazy { newIntentBuilder<TvEntryActivity>() }
}
