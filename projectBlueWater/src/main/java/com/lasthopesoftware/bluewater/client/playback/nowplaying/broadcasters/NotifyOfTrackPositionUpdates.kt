package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters

interface NotifyOfTrackPositionUpdates {
    fun updateTrackPosition(trackPosition: Long)
}
