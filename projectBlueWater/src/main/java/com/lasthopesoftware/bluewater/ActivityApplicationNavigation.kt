package com.lasthopesoftware.bluewater

import android.content.Intent
import android.os.Handler
import androidx.activity.ComponentActivity
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity.Companion.launchFileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.startItemBrowserActivity
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity.Companion.startNowPlayingActivity
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter

class ActivityApplicationNavigation(private val componentActivity: ComponentActivity) : NavigateApplication {

	private val handler by lazy { Handler(componentActivity.mainLooper) }
	private val selectServerIntent by lazy { Intent(componentActivity, ApplicationSettingsActivity::class.java) }
	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(componentActivity, BrowserEntryActivity::class.java)
		browseLibraryIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		browseLibraryIntent
	}

	override fun viewBrowserRoot(): Promise<Unit> = loopInOperation {
		componentActivity.startActivity(browseLibraryIntent)
	}

	override fun viewApplicationSettings() = loopInOperation {
		componentActivity.startActivity(selectServerIntent)
	}

	override fun viewFileDetails(playlist: List<ServiceFile>, position: Int) = loopInOperation {
		componentActivity.launchFileDetailsActivity(playlist, position)
	}

	override fun viewItem(libraryId: LibraryId, item: IItem) = loopInOperation {
		componentActivity.startItemBrowserActivity(libraryId, item)
	}

	override fun viewNowPlaying() = loopInOperation {
		componentActivity.startNowPlayingActivity()
	}

	override fun navigateUp() = loopInOperation { componentActivity.finish(); true }

	private fun <T> loopInOperation(operation: MessageWriter<T>) = LoopedInPromise(operation, handler)
}
