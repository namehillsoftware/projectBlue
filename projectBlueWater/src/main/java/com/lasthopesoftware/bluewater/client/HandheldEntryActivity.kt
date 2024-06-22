package com.lasthopesoftware.bluewater.client

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.shared.android.ui.ProjectBlueComposableApplication

@OptIn(UnstableApi::class)
class HandheldEntryActivity : EntryActivity() {
	override val intentBuilder by lazy { IntentBuilder(this, javaClass) }

	@Composable
	override fun Application(
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
