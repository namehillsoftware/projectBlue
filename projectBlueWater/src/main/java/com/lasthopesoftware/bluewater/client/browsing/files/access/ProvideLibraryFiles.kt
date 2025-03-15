package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibraryFiles {
	fun promiseFiles(libraryId: LibraryId): Promise<List<ServiceFile>>
	fun promiseAudioFiles(libraryId: LibraryId, query: String): Promise<List<ServiceFile>>
	fun promiseFiles(libraryId: LibraryId, itemId: ItemId): Promise<List<ServiceFile>>
	fun promiseFiles(libraryId: LibraryId, playlistId: PlaylistId): Promise<List<ServiceFile>>
}
