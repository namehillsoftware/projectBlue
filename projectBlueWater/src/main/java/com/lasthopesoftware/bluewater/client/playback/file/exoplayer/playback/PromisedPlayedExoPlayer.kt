package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.playback

import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.ExoPlayerFileProgressReader
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.lazyj.Lazy
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder
import org.slf4j.LoggerFactory
import java.io.EOFException
import java.net.ProtocolException

class PromisedPlayedExoPlayer(private val exoPlayer: PromisingExoPlayer, private val progressReader: ExoPlayerFileProgressReader, private val handler: ExoPlayerPlaybackHandler) : ProgressedPromise<Duration, PlayedFile>(), PlayedFile, Player.EventListener {

	companion object {
		private val minutesAndSecondsFormatter = Lazy {
			PeriodFormatterBuilder()
				.appendMinutes()
				.appendSeparator(":")
				.minimumPrintedDigits(2)
				.maximumParsedDigits(2)
				.appendSeconds()
				.toFormatter()
		}

		private val logger = LoggerFactory.getLogger(PromisedPlayedExoPlayer::class.java)
	}

	init {
		exoPlayer.addListener(this)
	}

	override val progress: Promise<Duration>
		get() = progressReader.progress

	override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
		if (playbackState == Player.STATE_IDLE && handler.isPlaying) {
			val formatter = minutesAndSecondsFormatter.getObject()
			progress.then { p ->
				handler.duration.then handler@{ d ->
					logger.warn(
						"The player was playing, but it transitioned to idle! " +
							"Playback progress: " + p.toPeriod().toString(formatter) + " / " + d.toPeriod().toString(formatter) + ". ")
					if (playWhenReady) {
						logger.warn("The file is set to playWhenReady, waiting for playback to resume.")
						return@handler
					}

					logger.warn("The file is not set to playWhenReady, triggering playback completed")
					removeListener()
					resolve(this)
				}
			}
			return
		}

		if (playbackState != Player.STATE_ENDED) return

		removeListener()
		resolve(this)
	}

	override fun onPlayerError(error: ExoPlaybackException) {
		removeListener()
		when (val cause = error.cause) {
			is EOFException -> {
				logger.warn("The file ended unexpectedly. Completing playback", error)
				resolve(this)
				return
			}
			is NoSuchElementException -> {
				logger.warn("The player was unexpectedly unable to dequeue messages, completing playback", error)
				resolve(this)
				return
			}
			is ProtocolException -> {
				val message = cause.message
				if (message != null && message.contains("unexpected end of stream")) {
					logger.warn("The stream ended unexpectedly, completing playback", error)
					resolve(this)
					return
				}
			}
			is InvalidResponseCodeException -> {
				if (cause.responseCode == 416) {
					logger.warn("Received an error code of " + cause.responseCode + ", completing playback", cause)
					resolve(this)
					return
				}
			}
		}

		logger.error("A player error has occurred", error)
		reject(ExoPlayerException(handler, error))
	}

	private fun removeListener() = exoPlayer.removeListener(this)

	override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {}
	override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}
	override fun onLoadingChanged(isLoading: Boolean) {}
	override fun onRepeatModeChanged(repeatMode: Int) {}
	override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
	override fun onPositionDiscontinuity(reason: Int) {}
	override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
	override fun onSeekProcessed() {}
}
