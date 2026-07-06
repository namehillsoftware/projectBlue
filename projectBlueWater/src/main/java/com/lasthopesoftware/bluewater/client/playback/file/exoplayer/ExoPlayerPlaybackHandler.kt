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
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileDuration
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.promises.ResolvedPromiseBox
import com.lasthopesoftware.promises.extensions.ProgressedPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder
import java.io.EOFException
import java.net.ProtocolException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException

class ExoPlayerPlaybackHandler(private val exoPlayer: PromisingExoPlayer) :
	ProgressedPromise<Duration, PlayedFile>(),
	PlayableFile,
	PlayingFile,
	PlayedFile,
	Player.Listener,
	CancellationResponse {

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

	private object LongDurationTransformer : ImmediateResponse<Long, Duration> {
		override fun respond(currentPosition: Long): Duration = Duration.millis(currentPosition)
	}

	private val fileProgressReader =
		AtomicReference<CloseableReadFileProgress>(PausedExoPlayerFileProgressReader(exoPlayer))

	private val promisedDuration by lazy { PlayingExoPlayerFileDurationReader(exoPlayer) }

	init {
		awaitCancellation(this)
		exoPlayer.addListener(this)
	}

	private fun pause(): Promise<PromisingExoPlayer> {
		isPlaying = false
		return exoPlayer.setPlayWhenReady(false)
	}

	private var isPlaying = false

	override fun promisePause(): Promise<PlayableFile> {
		val previousReader = fileProgressReader.getAndSet(PausedExoPlayerFileProgressReader(exoPlayer))
		previousReader.close()
		return pause().then { _ -> this }
	}

	override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> = this

	override val progress: Promise<Duration>
		get() = fileProgressReader.get().progress

	override val duration: Promise<Duration>
		get() = promisedDuration.duration

	override fun promisePlayback(): Promise<PlayingFile> {
		isPlaying = true
		val previousReader = fileProgressReader.getAndSet(PlayingExoPlayerFileProgressReader(exoPlayer))
		previousReader.close()
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
							"Playback progress: " + p.toPeriod().toString(formatter) + " / " + d.toPeriod()
							.toString(formatter) + ". "
					)
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

	@OptIn(UnstableApi::class)
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
				when (cause.message) {
					"unexpected end of stream" -> {
						logger.warn("The stream ended unexpectedly, completing playback", error)
						resolve(this)
						return
					}
				}
			}

			is ParserException -> {
				if (cause.message.startsWith("Searched too many bytes.")) {
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
		promisedDuration.close()
		fileProgressReader.get().close()
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

	private interface CloseableReadFileProgress : ReadFileProgress, AutoCloseable

	private class PausedExoPlayerFileProgressReader(private val exoPlayer: PromisingExoPlayer) :
		CloseableReadFileProgress {
		private val promisedFileProgress = RetryOnRejectionLazyPromise {
			exoPlayer.getCurrentPosition().then(LongDurationTransformer)
		}

		override val progress: Promise<Duration>
			get() = promisedFileProgress.value

		override fun close() {
			promisedFileProgress.close()
		}
	}

	private class PlayingExoPlayerFileProgressReader(private val exoPlayer: PromisingExoPlayer) :
		CloseableReadFileProgress {
		@Volatile
		private var isClosed = false

		private val currentProgressPromise = AtomicReference(0L.toPromise())

		override val progress: Promise<Duration>
			get() =
				if (isClosed) currentProgressPromise.get().then(LongDurationTransformer)
				else currentProgressPromise.updateAndGet { prev ->
					prev.cancel()
					exoPlayer.getCurrentPosition()
				}.then(LongDurationTransformer)

		override fun close() {
			isClosed = true
			currentProgressPromise.get().cancel()
		}
	}

	private class PlayingExoPlayerFileDurationReader(private val exoPlayer: PromisingExoPlayer) :
		ReadFileDuration, AutoCloseable {
		@Volatile
		private var isClosed = false

		private val currentDurationPromise = AtomicReference(ResolvedPromiseBox(PositiveDurationPromise(exoPlayer)))

		override val duration: Promise<Duration>
			get() =
				if (isClosed) currentDurationPromise.get().originalPromise
				else currentDurationPromise.get().resolvedPromise ?: currentDurationPromise.updateAndGet { prev ->
					prev.run {
						if (resolvedPromise != null) this
						else {
							originalPromise.cancel()
							ResolvedPromiseBox(PositiveDurationPromise(exoPlayer))
						}
					}
				}.originalPromise

		override fun close() {
			isClosed = true
			currentDurationPromise.get().originalPromise.cancel()
		}
	}

	private class PositiveDurationPromise(exoPlayer: PromisingExoPlayer) : Proxy<Duration>(),
		ImmediateResponse<Duration, Unit> {
		init {
			val promisedDuration = exoPlayer.getDuration().then(LongDurationTransformer)
			doCancel(promisedDuration)
			promisedDuration.then(this)
			proxyRejection(promisedDuration)
		}

		override fun respond(position: Duration) {
			if (position.isLongerThan(Duration.ZERO))
				resolve(position)
		}
	}
}
