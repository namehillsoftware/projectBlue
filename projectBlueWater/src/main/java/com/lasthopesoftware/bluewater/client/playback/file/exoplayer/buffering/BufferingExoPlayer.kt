package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MediaSourceEventListener
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.buffering.BufferingPlaybackFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.executors.HandlerExecutor
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.io.IOException

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class BufferingExoPlayer(
	private val handlerExecutor: HandlerExecutor,
	private val mediaSource: MediaSource,
	private val exoPlayer: PromisingExoPlayer
) : Promise<BufferingPlaybackFile>(),
	BufferingPlaybackFile,
	MediaSourceEventListener,
	Player.Listener,
	CancellableMessageWriter<Unit>,
	Runnable,
	ImmediateResponse<Collection<Unit>, BufferingExoPlayer>
{

	companion object {
		private val logger by lazyLogger<BufferingExoPlayer>()
	}

	fun promiseSubscribedExoPlayer(): Promise<BufferingExoPlayer> = whenAll(
		QueuedPromise(this, handlerExecutor),
		exoPlayer.addListener(this).unitResponse()
	).then(this)

	override fun promiseBufferedPlaybackFile(): Promise<BufferingPlaybackFile> {
		return this
	}

	override fun onLoadCompleted(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
		resolve(this)
		removeListeners()
	}

	override fun onLoadError(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, error: IOException, wasCanceled: Boolean) {
		logger.error("An error occurred during playback buffering", error)
		reject(error)
		removeListeners()
	}

	override fun onPlayerError(error: PlaybackException) {
		logger.error("A player error occurred during playback buffering", error)
		reject(error)
		removeListeners()
	}

	override fun prepareMessage(cancellationSignal: CancellationSignal) {
		mediaSource.addEventListener(handlerExecutor.handler, this)
		exoPlayer.addListener(this)
	}

	override fun run() {
		mediaSource.removeEventListener(this)
	}

	private fun removeListeners() {
		handlerExecutor.execute(this)
		exoPlayer.removeListener(this)
	}

	override fun respond(resolution: Collection<Unit>): BufferingExoPlayer = this
}
