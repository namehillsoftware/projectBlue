package com.lasthopesoftware.bluewater.client.playback.engine.events

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile

fun interface OnPlayingFileChanged {
    fun onPlayingFileChanged(libraryId: LibraryId, positionedPlayingFile: PositionedPlayingFile?)
}
