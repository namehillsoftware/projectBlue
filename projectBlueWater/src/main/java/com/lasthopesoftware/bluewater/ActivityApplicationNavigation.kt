package com.lasthopesoftware.bluewater

import android.os.Handler
import androidx.activity.ComponentActivity
import com.lasthopesoftware.bluewater.about.AboutActivity
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectBrowserLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsActivity
import com.lasthopesoftware.bluewater.shared.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.shared.android.intents.startActivity
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter

class ActivityApplicationNavigation(
	private val componentActivity: ComponentActivity,
	private val intentBuilder: BuildIntents,
	private val libraryBrowserSelection: SelectBrowserLibrary,
	private val selectedLibraryId: ProvideSelectedLibraryId,
) : NavigateApplication {

	private val handler by lazy { Handler(componentActivity.mainLooper) }

	override fun browseLibrary(libraryId: LibraryId): Promise<Unit> =
		libraryBrowserSelection
			.selectBrowserLibrary(libraryId)
			.eventually(
				LoopedInPromise.response(
					{ InstantiateSelectedConnectionActivity.startNewConnection(componentActivity) },
					handler
				)
			)

	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildItemBrowserIntent(libraryId))
	}

	override fun viewApplicationSettings() = loopInOperation {
		componentActivity.startActivity<ApplicationSettingsActivity>()
	}

	override fun viewHiddenApplicationSettings() = loopInOperation {
		componentActivity.startActivity<HiddenSettingsActivity>()
	}

	override fun launchAboutActivity() = loopInOperation {
		componentActivity.startActivity<AboutActivity>()
	}

	override fun viewNewServerSettings(): Promise<Unit> = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildNewLibraryIntent())
	}

	override fun viewServerSettings(libraryId: LibraryId) = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildLibrarySettingsIntent(libraryId))
	}

	override fun viewFileDetails(playlist: List<ServiceFile>, position: Int) = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildFileDetailsIntent(playlist, position))
	}

	override fun viewItem(libraryId: LibraryId, item: IItem) = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildItemBrowserIntent(libraryId, item))
	}

	override fun viewNowPlaying() = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildNowPlayingIntent())
	}

	override fun navigateUp() = loopInOperation { componentActivity.finish(); true }

	private fun <T> loopInOperation(operation: MessageWriter<T>) = LoopedInPromise(operation, handler)
}
