package com.lasthopesoftware.bluewater

import androidx.activity.ComponentActivity
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity.Companion.launchFileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.startItemBrowserActivity
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity.Companion.startNowPlayingActivity

class ActivityApplicationNavigation(private val componentActivity: ComponentActivity) : NavigateApplication {
	override fun viewFileDetails(playlist: List<ServiceFile>, position: Int) {
		componentActivity.launchFileDetailsActivity(playlist, position)
	}

	override fun viewItem(libraryId: LibraryId, item: IItem) {
		componentActivity.startItemBrowserActivity(libraryId, item)
	}

	override fun viewNowPlaying() {
		componentActivity.startNowPlayingActivity()
	}

	override fun backOut(): Boolean {
		componentActivity.finish()
		return true
	}
}
