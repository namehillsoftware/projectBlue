package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation

import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.AudioRenderingEventListener
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.MetadataOutputLogger
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.TextOutputLogger
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.volume.AudioTrackVolumeManager
import com.lasthopesoftware.bluewater.client.playback.volume.EmptyVolumeManager
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.lazyj.Lazy
import org.slf4j.LoggerFactory
import java.util.concurrent.CancellationException

internal class PreparedExoPlayerPromise(
	private val context: Context,
	private val mediaSourceProvider: SpawnMediaSources,
	private val trackSelector: TrackSelector,
	private val loadControl: LoadControl,
	private val renderersFactory: RenderersFactory,
	private val handler: Handler,
	private val uri: Uri,
	private val prepareAt: Long) :
	Promise<PreparedPlayableFile>(),
	Player.EventListener,
	Runnable,
	ImmediateResponse<Throwable, Unit> {

	companion object {
		private val lazyTextOutputLogger = Lazy { TextOutputLogger() }
		private val lazyMetadataOutputLogger = Lazy { MetadataOutputLogger() }
		private val logger = LoggerFactory.getLogger(ExoPlayerPlaybackHandler::class.java)
	}

	private val cancellationToken = CancellationToken()

	private var exoPlayer: ExoPlayer? = null
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
			exoPlayer = null
			return
		}

		audioRenderers = renderersFactory.createRenderers(
			handler,
			null,
			if (DebugFlag.getInstance().isDebugCompilation) AudioRenderingEventListener() else null,
			lazyTextOutputLogger.getObject(),
			lazyMetadataOutputLogger.getObject(),
			null)
			.mapNotNull { it as? MediaCodecAudioRenderer }
			.toTypedArray()

		val newExoPlayer = ExoPlayerFactory.newInstance(
			context,
			audioRenderers,
			trackSelector,
			loadControl,
			handler.looper)

		exoPlayer = newExoPlayer;

		if (cancellationToken.isCancelled) return

		newExoPlayer.addListener(this)

		if (cancellationToken.isCancelled) return

		val mediaSource = mediaSourceProvider.getNewMediaSource(uri)
		val newBufferingExoPlayer = BufferingExoPlayer(handler, mediaSource)
		bufferingExoPlayer = newBufferingExoPlayer

		try {
			newExoPlayer.prepare(mediaSource)
		} catch (e: IllegalStateException) {
			reject(e)
		}

		newBufferingExoPlayer.promiseBufferedPlaybackFile().excuse(this)
	}

	override fun run() {
		cancellationToken.run()
		exoPlayer?.release()
		reject(CancellationException())
	}

	override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {}

	override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

	override fun onLoadingChanged(isLoading: Boolean) {}

	override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
		if (isResolved || cancellationToken.isCancelled) return

		if (playbackState != Player.STATE_READY) return

		if (exoPlayer!!.currentPosition < prepareAt) {
			exoPlayer!!.seekTo(prepareAt)
			return
		}

		isResolved = true
		exoPlayer!!.removeListener(this)
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

		when (error) {
			is ParserException -> {
				val emptyPlaybackHandler = EmptyPlaybackHandler(0)
				resolve(PreparedPlayableFile(emptyPlaybackHandler, EmptyVolumeManager(), emptyPlaybackHandler))
			}
			else -> reject(error)
		}
	}

	override fun respond(throwable: Throwable) {
		handleError(throwable)
	}

	override fun onRepeatModeChanged(repeatMode: Int) {}

	override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

	override fun onPositionDiscontinuity(reason: Int) {}

	override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}

	override fun onSeekProcessed() {}
}
