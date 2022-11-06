package com.lasthopesoftware.bluewater

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity.Companion.launchFileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.startItemBrowserActivity
import com.lasthopesoftware.bluewater.client.browsing.items.startItemSearchActivity
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity.Companion.startNowPlayingActivity

class ActivityApplicationNavigation(private val context: Context) : NavigateApplication {
	override fun viewFileDetails(playlist: List<ServiceFile>, position: Int) {
		context.launchFileDetailsActivity(playlist, position)
	}

	override fun launchSearch() {
		context.startItemSearchActivity()
	}

	override fun viewItem(item: IItem) {
		context.startItemBrowserActivity(item)
	}

	override fun viewNowPlaying() {
		context.startNowPlayingActivity()
	}
}
