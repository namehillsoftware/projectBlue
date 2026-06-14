package com.lasthopesoftware.bluewater.client

import com.lasthopesoftware.bluewater.AccessActivityState
import com.lasthopesoftware.promises.extensions.LaunchActivitiesForResults

interface ActivitySuppliedDependencies {
	val registeredActivityResultsLauncher: LaunchActivitiesForResults
	val activityStateAccess: AccessActivityState
}
