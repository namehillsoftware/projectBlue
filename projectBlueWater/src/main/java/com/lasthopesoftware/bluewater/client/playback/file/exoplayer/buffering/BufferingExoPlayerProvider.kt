package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering

import androidx.media3.exoplayer.source.MediaSource
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.resources.executors.HandlerExecutor
import com.namehillsoftware.handoff.promises.Promise

class BufferingExoPlayerProvider(private val handlerExecutor: HandlerExecutor) :
	ProvideBufferingExoPlayers {
	override fun promiseBufferingExoPlayer(mediaSource: MediaSource, player: PromisingExoPlayer): Promise<BufferingExoPlayer> {
		return BufferingExoPlayer(handlerExecutor, mediaSource, player).promiseSubscribedExoPlayer()
	}
}
