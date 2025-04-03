package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.namehillsoftware.handoff.promises.Promise

interface ManageNowPlayingState : GetNowPlayingState {
    fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying>
}
