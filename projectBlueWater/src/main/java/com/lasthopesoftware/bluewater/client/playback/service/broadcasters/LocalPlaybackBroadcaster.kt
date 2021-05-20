package com.lasthopesoftware.bluewater.client.playback.service.broadcasters

import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

class LocalPlaybackBroadcaster(private val sendMessages: SendMessages) : IPlaybackBroadcaster {
	override fun sendPlaybackBroadcast(broadcastMessage: String, libraryId: LibraryId, positionedFile: PositionedFile) {
		val playbackBroadcastIntent = Intent(broadcastMessage)
		val currentPlaylistPosition = positionedFile.playlistPosition
		val fileKey = positionedFile.serviceFile.key
		playbackBroadcastIntent
			.putExtra(PlaylistEvents.PlaylistParameters.playlistPosition, currentPlaylistPosition)
			.putExtra(PlaylistEvents.PlaybackFileParameters.fileLibraryId, libraryId.id)
			.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, fileKey)
		sendMessages.sendBroadcast(playbackBroadcastIntent)
	}
}
