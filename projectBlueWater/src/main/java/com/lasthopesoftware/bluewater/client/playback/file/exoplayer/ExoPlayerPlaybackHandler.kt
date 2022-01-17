package com.lasthopesoftware.bluewater.client.playback.file.exoplayer

import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.playback.PromisedPlayedExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.ExoPlayerFileProgressReader
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import org.slf4j.LoggerFactory

class ExoPlayerPlaybackHandler(private val exoPlayer: PromisingExoPlayer) : PlayableFile, PlayingFile, Player.Listener, Runnable {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(ExoPlayerPlaybackHandler::class.java) }
	}

	private val lazyFileProgressReader by lazy { ExoPlayerFileProgressReader(exoPlayer) }

	private val exoPlayerPositionSource by lazy {
			PromisedPlayedExoPlayer(
				exoPlayer,
				lazyFileProgressReader,
				this)
		}

	private var backingDuration = Duration.ZERO

	init {
		exoPlayer.addListener(this)
	}

	private fun pause(): Promise<PromisingExoPlayer> {
		isPlaying = false
		return exoPlayer.setPlayWhenReady(false)
	}

	var isPlaying = false
		private set

	override fun promisePause(): Promise<PlayableFile> {
		return pause().then { this }
	}

	override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> = exoPlayerPositionSource

	override val progress: Promise<Duration>
		get() = lazyFileProgressReader.progress

	override val duration: Promise<Duration>
		get() = exoPlayer.getDuration().then { newDuration ->
			if (newDuration == backingDuration.millis) backingDuration
			else Duration.millis(newDuration).also { backingDuration = it }
		}

	override fun promisePlayback(): Promise<PlayingFile> {
		isPlaying = true
		return exoPlayer.setPlayWhenReady(true).then { this }
	}

	override fun close() {
		isPlaying = false
		exoPlayer.setPlayWhenReady(false)
		exoPlayer.stop()
		exoPlayer.release()
	}

	override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
		logger.debug("Tracks changed")
	}

	override fun onLoadingChanged(isLoading: Boolean) {
		logger.debug("Loading changed to $isLoading")
	}

	override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
		logger.debug("Playback state has changed to $playbackState")
		if (playbackState != Player.STATE_ENDED) return
		isPlaying = false
		exoPlayer.removeListener(this)
	}

	override fun onRepeatModeChanged(repeatMode: Int) {
		logger.debug("Repeat mode has changed")
	}

	override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
		logger.debug("Playback parameters have changed")
	}

	override fun run() {
		close()
	}
}
