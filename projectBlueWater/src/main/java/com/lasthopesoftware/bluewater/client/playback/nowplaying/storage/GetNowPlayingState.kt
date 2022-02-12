package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.namehillsoftware.handoff.promises.Promise

interface GetNowPlayingState {
	fun promiseNowPlaying(): Promise<NowPlaying?>
}
