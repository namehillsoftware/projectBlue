package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.namehillsoftware.handoff.promises.Promise

interface INowPlayingRepository {
    fun promiseNowPlaying(): Promise<NowPlaying?>
    fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying>
}
