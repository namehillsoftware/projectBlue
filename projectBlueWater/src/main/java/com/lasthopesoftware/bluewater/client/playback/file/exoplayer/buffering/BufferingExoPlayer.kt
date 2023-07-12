package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering

import android.os.Handler
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MediaSourceEventListener
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.Promise
import java.io.IOException

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class BufferingExoPlayer(handler: Handler, private val mediaSource: MediaSource, private val exoPlayer: PromisingExoPlayer) : Promise<IBufferingPlaybackFile>(), IBufferingPlaybackFile,
	MediaSourceEventListener, Player.Listener {

	companion object {
		private val logger by lazyLogger<BufferingExoPlayer>()
	}

	init {
		mediaSource.addEventListener(handler, this)
		exoPlayer.addListener(this)
	}

	override fun promiseBufferedPlaybackFile(): Promise<IBufferingPlaybackFile> {
		return this
	}

	override fun onLoadCompleted(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
		resolve(this)
		mediaSource.removeEventListener(this)
		exoPlayer.removeListener(this)
	}

	override fun onLoadError(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, error: IOException, wasCanceled: Boolean) {
		logger.error("An error occurred during playback buffering", error)
	}

	override fun onPlayerError(error: PlaybackException) {
		logger.error("A player error occurred during playback buffering", error)
		reject(error)
		mediaSource.removeEventListener(this)
		exoPlayer.removeListener(this)
	}
}
