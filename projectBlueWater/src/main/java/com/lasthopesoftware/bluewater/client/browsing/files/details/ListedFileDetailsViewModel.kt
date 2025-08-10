package com.lasthopesoftware.bluewater.client.browsing.files.details

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.namehillsoftware.handoff.promises.Promise

class ListedFileDetailsViewModel(
	private val playbackController: ControlPlaybackService,
	private val loadFileDetailsState: LoadFileDetailsState,
	private val fileDetailsState: FileDetailsState,
) : ViewModel(), PlayableFileDetailsState, FileDetailsState by fileDetailsState {
	private var activePosition: Int? = null
	private var activeFiles = emptyList<ServiceFile>()

	fun load(libraryId: LibraryId, files: List<ServiceFile>, position: Int): Promise<Unit> {
		activeFiles = files
		activePosition = position
		return loadFileDetailsState.load(libraryId, files[position])
	}

	override fun play() {
		playbackController.startPlaylist(
			libraryId = activeLibraryId ?: return,
			serviceFiles = activeFiles,
			position = activePosition ?: return,
		)
	}
}
