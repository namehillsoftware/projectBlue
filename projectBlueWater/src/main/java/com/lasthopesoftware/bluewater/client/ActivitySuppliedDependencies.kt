package com.lasthopesoftware.bluewater.client

import com.lasthopesoftware.bluewater.shared.promises.extensions.LaunchActivitiesForResults

interface ActivitySuppliedDependencies {
	val registeredActivityResultsLauncher: LaunchActivitiesForResults
}
