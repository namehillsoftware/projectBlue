package com.lasthopesoftware.bluewater.client.playback.service.notification

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile

interface NotifyOfPlaybackEvents {
    fun notifyPlaying()
    fun notifyPaused()
    fun notifyInterrupted()
    fun notifyStopped()
    fun notifyPlayingFileChanged(serviceFile: ServiceFile)
}
