package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

interface ChangePlaylistPosition {
	fun changePosition(playlistPosition: Int, filePosition: Duration): Promise<Pair<LibraryId, PositionedFile>>
	fun skipToNext(): Promise<Pair<LibraryId, PositionedFile>>
	fun skipToPrevious(): Promise<Pair<LibraryId, PositionedFile>>
}
