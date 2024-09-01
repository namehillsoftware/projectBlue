package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering

import android.os.Handler
import androidx.media3.exoplayer.source.MediaSource
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.namehillsoftware.handoff.promises.Promise

class BufferingExoPlayerProvider(private val playbackHandler: Handler, private val handler: Handler) :
	ProvideBufferingExoPlayers {
	override fun promiseBufferingExoPlayer(mediaSource: MediaSource, player: PromisingExoPlayer): Promise<BufferingExoPlayer> {
		return BufferingExoPlayer(playbackHandler, handler, mediaSource, player).promiseSubscribedExoPlayer()
	}
}
