package com.lasthopesoftware.bluewater.client.playback.exoplayer

import com.namehillsoftware.handoff.promises.Promise

interface ProvideExoPlayers {
	fun promiseExoPlayer(): Promise<PromisingExoPlayer>
}
