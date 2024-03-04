package com.lasthopesoftware.bluewater.client.playback.file.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.ParserException
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.ExoPlayerFileProgressReader
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.ProgressedPromise
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder
import java.io.EOFException
import java.net.ProtocolException
import kotlin.coroutines.cancellation.CancellationException

class ExoPlayerPlaybackHandler(private val exoPlayer: PromisingExoPlayer) :
	ProgressedPromise<Duration, PlayedFile>(),
	PlayableFile,
	PlayingFile,
	PlayedFile,
	Player.Listener,
	CancellationResponse
{

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

		private val logger by lazyLogger<ExoPlayerPlaybackHandler>()
	}

	private val fileProgressReader by lazy { ExoPlayerFileProgressReader(exoPlayer) }

	private var backingDuration = Duration.ZERO

	init {
		awaitCancellation(this)
		exoPlayer.addListener(this)
	}

	private fun pause(): Promise<PromisingExoPlayer> {
		isPlaying = false
		return exoPlayer.setPlayWhenReady(false)
	}

	private var isPlaying = false

	override fun promisePause(): Promise<PlayableFile> = pause().then { _ ->  this }

	override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> = this

	override val progress: Promise<Duration>
		get() = fileProgressReader.progress

	override val duration: Promise<Duration>
		get() = exoPlayer.getDuration().then { newDuration ->
			if (newDuration == backingDuration.millis) backingDuration
			else Duration.millis(newDuration).also { backingDuration = it }
		}

	override fun promisePlayback(): Promise<PlayingFile> {
		isPlaying = true
		return exoPlayer.setPlayWhenReady(true).then { _ -> this }
	}

	override fun onPlaybackStateChanged(playbackState: Int) {
		if (playbackState == Player.STATE_ENDED) {
			isPlaying = false
			removeListener()
			resolve(this)
			return
		}

		if (playbackState != Player.STATE_IDLE || !isPlaying) return

		progress
			.then { p ->
				duration.then { d ->
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

	@OptIn(UnstableApi::class) override fun onPlayerError(error: PlaybackException) {
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
				when (cause.message) {
					"unexpected end of stream" -> {
						logger.warn("The stream ended unexpectedly, completing playback", error)
						resolve(this)
						return
					}
				}
			}
			is ParserException -> {
				if (cause.message?.startsWith("Searched too many bytes.") == true) {
					logger.warn("The stream was corrupted, completing playback", error)
					resolve(this)
					return
				}
			}
			is HttpDataSource.InvalidResponseCodeException -> {
				if (cause.responseCode == 416) {
					logger.warn("Received an error code of " + cause.responseCode + ", completing playback", cause)
					resolve(this)
					return
				}
			}
		}

		logger.error("A player error has occurred", error)
		reject(ExoPlayerException(this, error))
	}

	override fun close() {
		isPlaying = false
		fileProgressReader.close()
		exoPlayer.setPlayWhenReady(false)
		exoPlayer.stop()
		removeListener()
		exoPlayer.release()
		reject(CancellationException("Playback resources closed before playback could complete."))
	}

	override fun cancellationRequested() {
		close()
	}

	private fun removeListener() = exoPlayer.removeListener(this)
}
