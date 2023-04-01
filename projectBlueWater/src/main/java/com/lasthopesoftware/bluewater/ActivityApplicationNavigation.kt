package com.lasthopesoftware.bluewater

import android.os.Handler
import androidx.activity.ComponentActivity
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectBrowserLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.InstantiateSelectedConnectionActivity
import com.lasthopesoftware.bluewater.shared.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter

class ActivityApplicationNavigation(
	private val componentActivity: ComponentActivity,
	private val intentBuilder: BuildIntents,
	private val libraryBrowserSelection: SelectBrowserLibrary,
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

	override fun viewFileDetails(playlist: List<ServiceFile>, position: Int) = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildFileDetailsIntent(playlist, position))
	}

	override fun viewNowPlaying() = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildNowPlayingIntent())
	}

	override fun navigateUp() = loopInOperation { componentActivity.finish(); true }

	private fun <T> loopInOperation(operation: MessageWriter<T>) = LoopedInPromise(operation, handler)
}
