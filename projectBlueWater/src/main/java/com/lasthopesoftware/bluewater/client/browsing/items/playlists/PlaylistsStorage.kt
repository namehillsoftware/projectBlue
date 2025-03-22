package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class PlaylistsStorage(private val libraryConnections: ProvideLibraryConnections) : StorePlaylists {
	override fun promiseAudioPlaylistPaths(libraryId: LibraryId): Promise<List<String>> = libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { libraryAccess ->
				libraryAccess?.promiseAudioPlaylistPaths().keepPromise(emptyList())
			}

	override fun promiseStoredPlaylist(libraryId: LibraryId, playlistPath: String, playlist: List<ServiceFile>): Promise<*> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { libraryAccess ->
				libraryAccess?.promiseStoredPlaylist(playlistPath, playlist).keepPromise()
			}
}
