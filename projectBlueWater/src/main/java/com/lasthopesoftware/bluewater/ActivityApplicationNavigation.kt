package com.lasthopesoftware.bluewater

import androidx.activity.ComponentActivity
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity.Companion.launchFileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.startItemBrowserActivity
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity.Companion.startNowPlayingActivity
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class ActivityApplicationNavigation(private val componentActivity: ComponentActivity) : NavigateApplication {
	override fun viewFileDetails(playlist: List<ServiceFile>, position: Int) {
		componentActivity.launchFileDetailsActivity(playlist, position)
	}

	override fun viewItem(item: IItem) {
		componentActivity.startItemBrowserActivity(item)
	}

	override fun viewNowPlaying() {
		componentActivity.startNowPlayingActivity()
	}

	override fun viewConnectionRestoration(): Promise<Unit> =
		InstantiateSelectedConnectionActivity.restoreSelectedConnection(componentActivity).unitResponse()

	override fun backOut(): Boolean {
		componentActivity.finish()
		return true
	}
}
