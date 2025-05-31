package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

interface PlaybackMessage : ApplicationMessage {

	object PlaybackPaused : PlaybackMessage

	object PlaybackInterrupted : PlaybackMessage

	object PlaybackStopped : PlaybackMessage

	object PlaybackStarting : PlaybackMessage

	object PlaybackStarted : PlaybackMessage
}

interface LibraryPlaybackMessage : PlaybackMessage {
	val libraryId: LibraryId

	class TrackChanged(override val libraryId: LibraryId, val positionedFile: PositionedFile) : LibraryPlaybackMessage

	data class TrackCompleted(override val libraryId: LibraryId, val completedFile: ServiceFile) : LibraryPlaybackMessage

	class TrackStarted(override val libraryId: LibraryId, val startedFile: ServiceFile) : LibraryPlaybackMessage

	class PlaylistChanged(override val libraryId: LibraryId) : LibraryPlaybackMessage
}
