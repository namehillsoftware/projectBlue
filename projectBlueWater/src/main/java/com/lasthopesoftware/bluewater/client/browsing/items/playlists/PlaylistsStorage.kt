package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.access.ProvideRemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class PlaylistsStorage(private val libraryConnections: ProvideRemoteLibraryAccess) : StorePlaylists {
	override fun promiseAudioPlaylistPaths(libraryId: LibraryId): Promise<List<String>> = libraryConnections
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { libraryAccess ->
				libraryAccess?.promiseAudioPlaylistPaths().keepPromise(emptyList())
			}

	override fun promiseStoredPlaylist(libraryId: LibraryId, playlistPath: String, playlist: List<ServiceFile>): Promise<*> =
		libraryConnections
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { libraryAccess ->
				libraryAccess?.promiseStoredPlaylist(playlistPath, playlist).keepPromise()
			}
}
