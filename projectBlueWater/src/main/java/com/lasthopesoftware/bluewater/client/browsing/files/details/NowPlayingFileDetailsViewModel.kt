package com.lasthopesoftware.bluewater.client.browsing.files.details

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingFileDetailsViewModel(
	private val controlPlayback: ControlPlaybackService,
	private val loadFileDetailsState: LoadFileDetailsState,
	private val fileDetailsState: FileDetailsState
) : ViewModel(), NowPlayingFileDetailsState, PlayableFileDetailsState, FileDetailsState by fileDetailsState {
	private var activePositionedFile: PositionedFile? = null

	fun load(libraryId: LibraryId, positionedFile: PositionedFile): Promise<Unit> {
		activePositionedFile = positionedFile
		return loadFileDetailsState.load(libraryId, positionedFile.serviceFile)
	}

	override fun removeFile() {
		val libraryId = activeLibraryId ?: return
		val positionedFile = activePositionedFile ?: return

		controlPlayback.removeFromPlaylistAtPosition(libraryId, positionedFile.playlistPosition)
	}

	override fun play() {
		val libraryId = activeLibraryId ?: return
		val positionedFile = activePositionedFile ?: return

		controlPlayback.seekTo(libraryId, positionedFile.playlistPosition)
	}
}
