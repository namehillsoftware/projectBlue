package com.lasthopesoftware.bluewater.client.playback.playlist

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Observable

interface ManagePlaylistPlayback {
	fun prepare(): ManagePlaylistPlayback
	fun pause(): Promise<*>
	fun resume(): Promise<PositionedPlayingFile?>
	fun setVolume(volume: Float): Promise<Unit>

	fun haltPlayback(): Promise<*>

	fun observe(): Observable<PositionedPlayingFile>

	val isPlaying: Boolean
}
