package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering

import androidx.media3.exoplayer.source.MediaSource
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.namehillsoftware.handoff.promises.Promise

interface ProvideBufferingExoPlayers {
	fun promiseBufferingExoPlayer(mediaSource: MediaSource, player: PromisingExoPlayer): Promise<BufferingExoPlayer>
}
