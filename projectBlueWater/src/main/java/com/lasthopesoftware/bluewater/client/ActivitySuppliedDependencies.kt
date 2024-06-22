package com.lasthopesoftware.bluewater.client

import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.promises.extensions.LaunchActivitiesForResults

interface ActivitySuppliedDependencies {
	val registeredActivityResultsLauncher: LaunchActivitiesForResults
	val intentBuilder: BuildIntents
}
