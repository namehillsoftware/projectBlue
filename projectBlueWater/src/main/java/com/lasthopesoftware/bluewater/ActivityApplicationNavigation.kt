package com.lasthopesoftware.bluewater

import android.os.Handler
import androidx.activity.ComponentActivity
import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.HandlerExecutor
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter

class ActivityApplicationNavigation(
    private val componentActivity: ComponentActivity,
    private val intentBuilder: BuildIntents,
) : NavigateApplication {

	private val executor by lazy { HandlerExecutor(Handler(componentActivity.mainLooper)) }

	override fun viewLibrary(libraryId: LibraryId) = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildViewLibraryIntent(libraryId))
	}

	override fun search(libraryId: LibraryId, filePropertyFilter: FileProperty): Promise<Unit> = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildLibrarySearchIntent(libraryId, filePropertyFilter))
	}

	override fun viewApplicationSettings() = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildApplicationSettingsIntent())
	}

	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int) = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildFileDetailsIntent(libraryId, playlist, position))
	}

	override fun viewNowPlaying(libraryId: LibraryId) = loopInOperation {
		componentActivity.startActivity(intentBuilder.buildNowPlayingIntent(libraryId))
	}

	override fun navigateUp() = loopInOperation { componentActivity.finish(); true }

	private fun <T> loopInOperation(operation: CancellableMessageWriter<T>) = executor.preparePromise(operation)
}
