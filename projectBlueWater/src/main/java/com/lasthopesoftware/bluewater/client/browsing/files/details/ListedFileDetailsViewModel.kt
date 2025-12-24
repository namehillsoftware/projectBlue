package com.lasthopesoftware.bluewater.client.browsing.files.details

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

abstract class ListedFileDetailsViewModel<Item>(
	private val playbackController: ControlPlaybackService,
	private val loadFileDetailsState: LoadFileDetailsState,
	private val fileDetailsState: FileDetailsState,
) : ViewModel(), PlayableFileDetailsState, FileDetailsState by fileDetailsState {
	private var activePosition: Int? = null
	private var activeFiles = emptyList<ServiceFile>()

	private val mutableIsPlayableWithPlaylist = MutableInteractionState(false)

	override val isPlayableWithPlaylist = mutableIsPlayableWithPlaylist.asInteractionState()

	fun load(libraryId: LibraryId, item: Item, positionedFile: PositionedFile): Promise<Unit> {
		val (position, serviceFile) = positionedFile

		val promisedLibraryFiles = promiseFiles(libraryId, item)
			.cancelBackThen { f, cs ->
				activeFiles = f
				if (!cs.isCancelled) {
					activePosition = f.elementAtOrNull(position)
						?.takeIf { it == serviceFile }
						?.let { position }
					mutableIsPlayableWithPlaylist.value = activePosition != null
				}
			}

		val promisedFileDetails = loadFileDetailsState.load(libraryId, positionedFile.serviceFile)

		return Promise.whenAll(promisedLibraryFiles, promisedFileDetails).unitResponse()
	}

	protected abstract fun promiseFiles(libraryId: LibraryId, item: Item): Promise<List<ServiceFile>>

	override fun play() {
		playbackController.startPlaylist(
			libraryId = activeLibraryId ?: return,
			serviceFiles = activeFiles,
			position = activePosition ?: return,
		)
	}
}
