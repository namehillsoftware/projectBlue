package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

interface ChangePlaylistPosition {
	fun changePosition(playlistPosition: Int, filePosition: Duration): Promise<PositionedFile>
	fun skipToNext(): Promise<PositionedFile>
	fun skipToPrevious(): Promise<PositionedFile>
}
