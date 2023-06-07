package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

interface NotifyOfPlaybackEvents {
	fun updateLibrary(libraryId: LibraryId)
    fun notifyPlaying()
    fun notifyPaused()
    fun notifyInterrupted()
    fun notifyStopped()
    fun notifyPlayingFileUpdated(serviceFile: ServiceFile)
}
