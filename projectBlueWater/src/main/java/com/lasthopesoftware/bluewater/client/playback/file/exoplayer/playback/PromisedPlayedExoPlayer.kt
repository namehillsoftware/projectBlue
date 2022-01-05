package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.playback

import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.ExoPlayerFileProgressReader
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder
import org.slf4j.LoggerFactory
import java.io.EOFException
import java.net.ProtocolException

class PromisedPlayedExoPlayer(private val exoPlayer: PromisingExoPlayer, private val progressReader: ExoPlayerFileProgressReader, private val handler: ExoPlayerPlaybackHandler) : ProgressedPromise<Duration, PlayedFile>(), PlayedFile, Player.Listener {

	companion object {
		private val minutesAndSecondsFormatter by lazy {
			PeriodFormatterBuilder()
				.appendMinutes()
				.appendSeparator(":")
				.minimumPrintedDigits(2)
				.maximumParsedDigits(2)
				.appendSeconds()
				.toFormatter()
		}

		private val logger by lazy { LoggerFactory.getLogger(PromisedPlayedExoPlayer::class.java) }
	}

	init {
		exoPlayer.addListener(this)
	}

	override val progress: Promise<Duration>
		get() = progressReader.progress

	override fun onPlaybackStateChanged(playbackState: Int) {
		if (playbackState == Player.STATE_ENDED) {
			removeListener()
			resolve(this)
			return
		}

		if (playbackState != Player.STATE_IDLE || !handler.isPlaying) return

		progress
			.then { p ->
				handler.duration.then handler@{ d ->
					val formatter = minutesAndSecondsFormatter
					logger.warn(
						"The player was playing, but it transitioned to idle! " +
							"Playback progress: " + p.toPeriod().toString(formatter) + " / " + d.toPeriod().toString(formatter) + ". ")
				}
			}
			.eventually { exoPlayer.getPlayWhenReady() }
			.then { ready ->
				if (ready) {
					logger.warn("The file is set to playWhenReady, waiting for playback to resume.")
				} else {
					logger.warn("The file is not set to playWhenReady, triggering playback completed")
					removeListener()
					resolve(this)
				}
			}
	}

	override fun onPlayerError(error: PlaybackException) {
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
}
