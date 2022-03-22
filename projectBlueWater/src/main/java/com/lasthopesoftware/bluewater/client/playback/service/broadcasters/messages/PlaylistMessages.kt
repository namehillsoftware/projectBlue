package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

data class PlaylistTrackChange(
	val libraryId: LibraryId,
	val positionedFile: PositionedFile,
) : ApplicationMessage
