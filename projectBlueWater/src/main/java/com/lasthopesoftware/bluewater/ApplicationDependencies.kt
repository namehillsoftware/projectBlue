package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.shared.android.intents.BuildIntents

interface ApplicationDependencies {
	val intentBuilder: BuildIntents
	val syncScheduler: SyncScheduler
}
