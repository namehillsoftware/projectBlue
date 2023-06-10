package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface StorePlaylists {
	fun promiseAudioPlaylistPaths(libraryId: LibraryId): Promise<List<String>>
	fun promiseStoredPlaylist(libraryId: LibraryId, playlistPath: String, playlist: List<ServiceFile>): Promise<*>
}
