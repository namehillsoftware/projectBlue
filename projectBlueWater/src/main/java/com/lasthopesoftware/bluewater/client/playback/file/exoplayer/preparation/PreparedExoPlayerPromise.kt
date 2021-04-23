package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.lasthopesoftware.bluewater.client.playback.exoplayer.HandlerDispatchingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.rendering.GetAudioRenderers
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.volume.AudioTrackVolumeManager
import com.lasthopesoftware.bluewater.client.playback.volume.PassthroughVolumeManager
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.util.concurrent.CancellationException

internal class PreparedExoPlayerPromise(
	private val context: Context,
	private val mediaSourceProvider: SpawnMediaSources,
	private val loadControl: LoadControl,
	private val renderersFactory: GetAudioRenderers,
	private val playbackHandler: Handler,
	private val playbackControlHandler: Handler,
	private val eventHandler: Handler,
	private val uri: Uri,
	private val prepareAt: Duration) :
	Promise<PreparedPlayableFile>(),
	Player.EventListener,
	ImmediateResponse<Array<MediaCodecAudioRenderer>, Unit>,
	Runnable {

	companion object {
		private val logger = LoggerFactory.getLogger(PreparedExoPlayerPromise::class.java)
	}

	private val cancellationToken = CancellationToken()

	private var exoPlayer: PromisingExoPlayer? = null
	private var audioRenderers: Array<MediaCodecAudioRenderer> = emptyArray()
	private var bufferingExoPlayer: BufferingExoPlayer? = null
	private var isResolved = false

	init {
		initialize()
	}

	private fun initialize() {
		respondToCancellation(this)

		if (cancellationToken.isCancelled) {
			reject(CancellationException())
			return
		}

		renderersFactory.newRenderers().then(this, ::handleError)
	}

	override fun respond(renderers: Array<MediaCodecAudioRenderer>) {
		audioRenderers = renderers
		val exoPlayerBuilder = ExoPlayer.Builder(context, *renderers)
			.setLoadControl(loadControl)
			.setLooper(playbackHandler.looper)

		val newExoPlayer = HandlerDispatchingExoPlayer(exoPlayerBuilder.build(), playbackControlHandler)
		exoPlayer = newExoPlayer

		if (cancellationToken.isCancelled) return

		newExoPlayer
			.addListener(this)
			.eventually {
				if (cancellationToken.isCancelled) {
					empty()
				} else {
					val mediaSource = mediaSourceProvider.getNewMediaSource(uri)

					val newBufferingExoPlayer = BufferingExoPlayer(eventHandler, mediaSource)
					bufferingExoPlayer = newBufferingExoPlayer

					val prepareAtMillis = prepareAt.millis
					if (prepareAtMillis == 0L) {
						newExoPlayer.setMediaSource(mediaSource)
					} else {
						newExoPlayer
							.setMediaSource(mediaSource, prepareAtMillis)
							.eventually { newExoPlayer.seekTo(prepareAtMillis) }
					}.eventually {
						newExoPlayer.prepare()
					}.eventually {
						newBufferingExoPlayer.promiseBufferedPlaybackFile()
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

	override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
		if (isResolved || cancellationToken.isCancelled) return

		if (playbackState != Player.STATE_READY) return

		val exoPlayer = exoPlayer ?: return

		isResolved = true
		exoPlayer.removeListener(this)
		resolve(
			PreparedPlayableFile(
				ExoPlayerPlaybackHandler(exoPlayer),
				AudioTrackVolumeManager(exoPlayer, audioRenderers),
				bufferingExoPlayer))
	}

	override fun onPlayerError(error: ExoPlaybackException) {
		handleError(error)
	}

	private fun handleError(error: Throwable) {
		if (isResolved) return

		isResolved = true

		logger.error("An error occurred while preparing the exo player!", error)

		exoPlayer?.stop()
		exoPlayer?.release()

		when (error.cause) {
			is ParserException -> {
				logger.warn("A parser exception occurred while preparing the file, skipping playback", error)
				val emptyPlaybackHandler = EmptyPlaybackHandler(0)
				resolve(PreparedPlayableFile(emptyPlaybackHandler, PassthroughVolumeManager(), emptyPlaybackHandler))
			}
			else -> reject(error)
		}
	}

	override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {}

	override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

	override fun onLoadingChanged(isLoading: Boolean) {}

	override fun onRepeatModeChanged(repeatMode: Int) {}

	override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

	override fun onPositionDiscontinuity(reason: Int) {}

	override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}

	override fun onSeekProcessed() {}
}
