package com.namehillsoftware.projectblue.tv.client

import androidx.compose.runtime.Composable
import com.lasthopesoftware.bluewater.client.EntryActivity
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.shared.android.ui.ProjectBlueComposableApplication
import com.namehillsoftware.projectblue.tv.client.playback.nowplaying.view.NowPlayingTvApplication

class TvEntryActivity : EntryActivity() {
	@Composable
	override fun application(
		browserViewDependencies: BrowserViewDependencies,
		permissionsDependencies: PermissionsDependencies,
		initialDestination: Destination?
	) {
		ProjectBlueComposableApplication(darkTheme = true) {
			NowPlayingTvApplication(
				browserViewDependencies = browserViewDependencies,
				permissionsDependencies = permissionsDependencies,
				initialDestination = initialDestination,
			)
		}
	}
}
