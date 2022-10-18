package com.lasthopesoftware.bluewater.client.playback.service.notification

interface NotifyOfPlaybackEvents {
    fun notifyPlaying()
    fun notifyPaused()
    fun notifyInterrupted()
    fun notifyStopped()
    fun notifyPlayingFileUpdated()
}
