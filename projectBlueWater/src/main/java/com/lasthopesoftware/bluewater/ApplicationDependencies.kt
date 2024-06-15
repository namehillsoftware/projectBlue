package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler

interface ApplicationDependencies {
	val intentBuilder: BuildIntents
	val syncScheduler: SyncScheduler
}
