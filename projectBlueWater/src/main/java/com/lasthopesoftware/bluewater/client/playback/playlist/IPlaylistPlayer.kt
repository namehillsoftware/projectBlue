package com.lasthopesoftware.bluewater.client.playback.playlist

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.ObservableOnSubscribe

/**
 * Created by david on 11/7/16.
 */
interface IPlaylistPlayer : ObservableOnSubscribe<PositionedPlayingFile> {
	fun pause(): Promise<*>
	fun resume(): Promise<PositionedPlayingFile?>
	fun setVolume(volume: Float): Promise<Unit>

	fun haltPlayback(): Promise<*>

	val isPlaying: Boolean
}
