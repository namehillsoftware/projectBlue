package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.namehillsoftware.handoff.promises.Promise
import java.io.IOException

interface ChangePlaylistPosition {
	@Throws(IOException::class)
	fun changePosition(playlistPosition: Int, filePosition: Int): Promise<PositionedFile>
	fun skipToNext(): Promise<PositionedFile>
	fun skipToPrevious(): Promise<PositionedFile>
}
