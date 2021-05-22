package com.lasthopesoftware.bluewater.client.playback.service.broadcasters

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

interface IPlaybackBroadcaster {
	fun sendPlaybackBroadcast(broadcastMessage: String, libraryId: LibraryId, positionedFile: PositionedFile)
}
