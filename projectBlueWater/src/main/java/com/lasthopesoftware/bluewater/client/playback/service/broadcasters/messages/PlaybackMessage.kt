package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

interface PlaybackMessage : ApplicationMessage {

	class TrackChanged(val libraryId: LibraryId, val positionedFile: PositionedFile) : PlaybackMessage

	data class TrackCompleted(val libraryId: LibraryId, val completedFile: ServiceFile) : PlaybackMessage

	class TrackStarted(val libraryId: LibraryId, val startedFile: ServiceFile) : PlaybackMessage

	object PlaylistChanged : PlaybackMessage

	object PlaybackStarted : PlaybackMessage

	object PlaybackPaused : PlaybackMessage

	object PlaybackInterrupted : PlaybackMessage

	object PlaybackStopped : PlaybackMessage
}
