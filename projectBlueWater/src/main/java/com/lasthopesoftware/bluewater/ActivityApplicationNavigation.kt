package com.lasthopesoftware.bluewater

import android.content.Intent
import android.os.Handler
import androidx.activity.ComponentActivity
import com.lasthopesoftware.bluewater.about.AboutActivity
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity.Companion.launchFileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.startItemBrowserActivity
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectBrowserLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity.Companion.startNowPlayingActivity
import com.lasthopesoftware.bluewater.client.settings.IEditClientSettingsActivityIntentBuilder
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter

class ActivityApplicationNavigation(
	private val componentActivity: ComponentActivity,
	private val editClientSettingsIntentBuilder: IEditClientSettingsActivityIntentBuilder,
	private val libraryBrowserSelection: SelectBrowserLibrary
) : NavigateApplication {

	private val handler by lazy { Handler(componentActivity.mainLooper) }
	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(componentActivity, BrowserEntryActivity::class.java)
		browseLibraryIntent
	}

	override fun browseLibrary(libraryId: LibraryId): Promise<Unit> =
		libraryBrowserSelection
			.selectBrowserLibrary(libraryId)
			.eventually(
				LoopedInPromise.response(
					{ InstantiateSelectedConnectionActivity.startNewConnection(componentActivity) },
					handler
				)
			)

	override fun resetToBrowserRoot(): Promise<Unit> = loopInOperation {
		componentActivity.startActivity(browseLibraryIntent)
	}

	override fun viewApplicationSettings() = loopInOperation {
		ApplicationSettingsActivity.launch(componentActivity)
	}

	override fun launchAboutActivity() = loopInOperation {
		componentActivity.startActivity(Intent(componentActivity, AboutActivity::class.java))
	}

	override fun viewNewServerSettings(): Promise<Unit> = loopInOperation {
		componentActivity.startActivity(editClientSettingsIntentBuilder.buildIntent(LibraryId(-1)))
	}

	override fun viewServerSettings(libraryId: LibraryId) = loopInOperation {
		componentActivity.startActivity(editClientSettingsIntentBuilder.buildIntent(libraryId))
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
