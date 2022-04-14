package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.ParserException
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.volume.AudioTrackVolumeManager
import com.lasthopesoftware.bluewater.client.playback.volume.PassthroughVolumeManager
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.net.ProtocolException
import java.util.concurrent.CancellationException

internal class PreparedExoPlayerPromise(
	private val mediaSourceProvider: SpawnMediaSources,
	private val eventHandler: Handler,
	private val exoPlayers: ProvideExoPlayers,
	private val uri: Uri,
	private val prepareAt: Duration
) :
	Promise<PreparedPlayableFile>(),
	Player.Listener,
	Runnable {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(PreparedExoPlayerPromise::class.java) }
	}

	private val cancellationToken = CancellationToken()

	private lateinit var bufferingExoPlayer: BufferingExoPlayer
	private var exoPlayer: PromisingExoPlayer? = null
	private var isResolved = false

	init {
		try {
			initialize()
		} catch (e: Throwable) {
			handleError(e)
		}
	}

	private fun initialize() {
		respondToCancellation(this)

		if (cancellationToken.isCancelled) return

		val newExoPlayer = exoPlayers.getExoPlayer()
		if (cancellationToken.isCancelled) return

		exoPlayer = newExoPlayer
		newExoPlayer
			.addListener(this)
			.eventually {
				if (cancellationToken.isCancelled) {
					empty()
				} else {
					mediaSourceProvider
						.promiseNewMediaSource(uri)
						.eventually { mediaSource ->
							val newBufferingExoPlayer = BufferingExoPlayer(eventHandler, mediaSource, it)
							bufferingExoPlayer = newBufferingExoPlayer

							val prepareAtMillis = prepareAt.millis

							val preparedExoPlayerPromise = if (prepareAtMillis == 0L) {
								it.setMediaSource(mediaSource)
							} else {
								it
									.setMediaSource(mediaSource, prepareAtMillis)
									.eventually { e -> e.seekTo(prepareAtMillis) }
							}

							preparedExoPlayerPromise
								.eventually { e ->
									e.prepare()
								}
								.eventually {
									newBufferingExoPlayer.promiseBufferedPlaybackFile()
								}
						}
				}
			}
			.excuse(::handleError)
	}

	override fun run() {
		cancellationToken.run()
		exoPlayer?.release()
		reject(CancellationException())
	}

	override fun onPlaybackStateChanged(playbackState: Int) {
		if (isResolved || cancellationToken.isCancelled) return

		if (playbackState != Player.STATE_READY) return

		val exoPlayer = exoPlayer ?: return

		isResolved = true

		exoPlayer.removeListener(this)
		resolve(
			PreparedPlayableFile(
				ExoPlayerPlaybackHandler(exoPlayer),
				AudioTrackVolumeManager(exoPlayer),
				bufferingExoPlayer))
	}

	override fun onPlayerError(error: PlaybackException) {
		handleError(error)
	}

	private fun handleError(error: Throwable) {
		if (isResolved) return

		isResolved = true

		try {
			exoPlayer?.stop()
			exoPlayer?.release()
		} catch (e: Throwable) {
			reject(e)
			return
		}

		when (val cause = error.cause) {
			is ParserException -> {
				logger.warn("A parser exception occurred while preparing the file, skipping playback", error)
				val emptyPlaybackHandler = EmptyPlaybackHandler(0)
				resolve(PreparedPlayableFile(emptyPlaybackHandler, PassthroughVolumeManager(), emptyPlaybackHandler))
				return
			}
			is HttpDataSource.HttpDataSourceException -> {
				when (val httpCause = cause.cause) {
					is ProtocolException -> {
						if (httpCause.message == "unexpected end of stream") {
							logger.warn("The stream ended unexpectedly, skipping playback", error)
							val emptyPlaybackHandler = EmptyPlaybackHandler(0)
							resolve(
								PreparedPlayableFile(
									emptyPlaybackHandler,
									PassthroughVolumeManager(),
									emptyPlaybackHandler
								)
							)
							return
						}
					}
				}
			}
		}

		logger.error("An error occurred while preparing the exo player!", error)
		reject(error)
	}
}
