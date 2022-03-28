package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

data class PlaylistTrackChanged(val libraryId: LibraryId, val positionedFile: PositionedFile) : ApplicationMessage

data class PlaylistTrackCompleted(val completedFile: ServiceFile) : ApplicationMessage

object PlaybackStart : ApplicationMessage

object PlaylistChanged : ApplicationMessage

object PlaybackPaused : ApplicationMessage

object PlaybackInterrupted : ApplicationMessage

object PlaybackStopped : ApplicationMessage
