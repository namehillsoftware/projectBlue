package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile

class LinkedOnPlayingFileChangedListener(private val previous: OnPlayingFileChanged, private val next: OnPlayingFileChanged) : OnPlayingFileChanged {
	override fun onPlayingFileChanged(libraryId: LibraryId, positionedPlayingFile: PositionedPlayingFile?) {
		previous.onPlayingFileChanged(libraryId, positionedPlayingFile)
		next.onPlayingFileChanged(libraryId, positionedPlayingFile)
	}
}
