package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder

interface IRemoteBroadcaster {
    fun setPlaying()
    fun setPaused()
    fun setStopped()
    fun updateNowPlaying(serviceFile: ServiceFile)
    fun filePropertiesUpdated(updatedKey: UrlKeyHolder<ServiceFile>)
    fun updateTrackPosition(trackPosition: Long)
}
