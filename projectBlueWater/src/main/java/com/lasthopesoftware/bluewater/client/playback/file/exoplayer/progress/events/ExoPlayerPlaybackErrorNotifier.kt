package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.events

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.client.playback.file.progress.NotifyFilePlaybackError

class ExoPlayerPlaybackErrorNotifier(private val handler: ExoPlayerPlaybackHandler) : NotifyFilePlaybackError<ExoPlayerException>, Player.EventListener {
	private var errorAction: ((ExoPlayerException) -> Unit)? = null

	override fun playbackError(onError: ((ExoPlayerException) -> Unit)?) {
		errorAction = onError
	}

	override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {}
	override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}
	override fun onLoadingChanged(isLoading: Boolean) {}
	override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}
	override fun onRepeatModeChanged(repeatMode: Int) {}
	override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
	override fun onPlayerError(error: ExoPlaybackException) {
		errorAction?.invoke(ExoPlayerException(handler, error))
	}

	override fun onPositionDiscontinuity(reason: Int) {}
	override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
	override fun onSeekProcessed() {}
}
