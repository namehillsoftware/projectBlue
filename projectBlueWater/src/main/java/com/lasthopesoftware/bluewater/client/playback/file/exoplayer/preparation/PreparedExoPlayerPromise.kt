package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.ProvideBufferingExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.volume.AudioTrackVolumeManager
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.cancellation.CancellationToken
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.util.concurrent.CancellationException

internal class PreparedExoPlayerPromise(
	private val mediaSourceProvider: SpawnMediaSources,
	private val bufferingExoPlayerProvider: ProvideBufferingExoPlayers,
	private val exoPlayers: ProvideExoPlayers,
	private val libraryId: LibraryId,
	private val uri: Uri,
	private val prepareAt: Duration
) :
	Promise<PreparedPlayableFile>(),
	Player.Listener,
	CancellationResponse {

	companion object {
		private val logger by lazyLogger<PreparedExoPlayerPromise>()
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
		awaitCancellation(this)

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
						.promiseNewMediaSource(libraryId, uri)
						.eventually { mediaSource ->
							bufferingExoPlayerProvider
								.promiseBufferingExoPlayer(mediaSource, it)
								.then { ep -> Pair(mediaSource, ep) }
						}
						.eventually { pair ->
							val (mediaSource, newBufferingExoPlayer) = pair
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

	override fun cancellationRequested() {
		cancellationToken.cancellationRequested()
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

	@OptIn(UnstableApi::class) private fun handleError(error: Throwable) {
		if (isResolved) return

		isResolved = true

		logger.error("An error occurred while preparing the exo player!", error)

		try {
			exoPlayer?.stop()
			exoPlayer?.release()
		} catch (e: Throwable) {
			logger.error("An error occurred while releasing the exo player!", e)
			reject(e)
			return
		}

		reject(error)
	}
}
