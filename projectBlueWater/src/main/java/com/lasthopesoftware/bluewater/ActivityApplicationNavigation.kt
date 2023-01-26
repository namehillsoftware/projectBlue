package com.lasthopesoftware.bluewater

import android.content.Intent
import androidx.activity.ComponentActivity
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity.Companion.launchFileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.startItemBrowserActivity
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity.Companion.startNowPlayingActivity
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity

class ActivityApplicationNavigation(private val componentActivity: ComponentActivity) : NavigateApplication {

	private val selectServerIntent by lazy { Intent(componentActivity, ApplicationSettingsActivity::class.java) }

	override fun launchSettings() {
		componentActivity.startActivity(selectServerIntent)
	}

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
