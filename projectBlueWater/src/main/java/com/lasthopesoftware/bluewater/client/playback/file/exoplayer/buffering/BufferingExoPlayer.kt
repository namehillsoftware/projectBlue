package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering

import android.os.Handler
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.Promise
import java.io.IOException

class BufferingExoPlayer(handler: Handler, private val mediaSource: MediaSource, private val exoPlayer: PromisingExoPlayer) : Promise<IBufferingPlaybackFile>(), IBufferingPlaybackFile, MediaSourceEventListener, Player.Listener {

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
