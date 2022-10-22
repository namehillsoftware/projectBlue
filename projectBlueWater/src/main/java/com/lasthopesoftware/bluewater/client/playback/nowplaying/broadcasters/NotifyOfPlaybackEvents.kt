package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters

interface NotifyOfPlaybackEvents {
    fun notifyPlaying()
    fun notifyPaused()
    fun notifyInterrupted()
    fun notifyStopped()
    fun notifyPlayingFileUpdated()
}
