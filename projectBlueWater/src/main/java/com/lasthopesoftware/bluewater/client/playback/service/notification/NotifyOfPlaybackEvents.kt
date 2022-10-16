package com.lasthopesoftware.bluewater.client.playback.service.notification

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder

interface NotifyOfPlaybackEvents {
    fun notifyPlaying()
    fun notifyPaused()
    fun notifyInterrupted()
    fun notifyStopped()
    fun notifyPlayingFileChanged(serviceFile: ServiceFile)
	fun notifyPropertiesUpdated(urlKeyHolder: UrlKeyHolder<ServiceFile>)
}
