package com.namehillsoftware.projectblue.handheld.client

import androidx.compose.runtime.Composable
import com.lasthopesoftware.bluewater.client.EntryActivity
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.shared.android.ui.ProjectBlueComposableApplication

class HandheldEntryActivity : EntryActivity() {
	@Composable
	override fun application(
		browserViewDependencies: BrowserViewDependencies,
		permissionsDependencies: PermissionsDependencies,
		initialDestination: Destination?
	) {
		ProjectBlueComposableApplication {
			HandheldApplication(
				browserViewDependencies = browserViewDependencies,
				permissionsDependencies = permissionsDependencies,
				initialDestination = initialDestination,
			)
		}
	}
}
