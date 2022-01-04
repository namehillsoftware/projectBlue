package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

interface ChangePlaybackState {
	fun restoreFromSavedState(): Promise<PositionedProgressedFile>
	fun startPlaylist(playlist: List<ServiceFile>, playlistPosition: Int, filePosition: Duration): Promise<Unit>
	fun resume(): Promise<Unit>
	fun pause(): Promise<Unit>
}
