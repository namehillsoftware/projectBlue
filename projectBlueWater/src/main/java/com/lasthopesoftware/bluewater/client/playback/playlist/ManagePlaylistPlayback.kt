package com.lasthopesoftware.bluewater.client.playback.playlist

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise

interface ManagePlaylistPlayback {
	fun pause(): Promise<PositionedPlayableFile?>
	fun resume(): Promise<PositionedPlayingFile?>
	fun setVolume(volume: Float): Promise<Unit>

	fun haltPlayback(): Promise<Unit>

	fun promisePlayedPlaylist(): ProgressingPromise<PositionedPlayingFile, Unit>

	val isPlaying: Boolean
}
