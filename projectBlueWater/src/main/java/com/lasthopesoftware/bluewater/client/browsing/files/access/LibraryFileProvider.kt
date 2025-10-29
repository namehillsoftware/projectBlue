package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryFileProvider(private val libraryConnections: ProvideLibraryConnections) : ProvideLibraryFiles {

	override fun promiseFiles(libraryId: LibraryId): Promise<List<ServiceFile>> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { it?.promiseFiles().keepPromise { emptyList() } }

	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId): Promise<List<ServiceFile>> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { it?.promiseFiles(itemId).keepPromise { emptyList() } }

	override fun promiseFiles(libraryId: LibraryId, playlistId: PlaylistId): Promise<List<ServiceFile>> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { it?.promiseFiles(playlistId).keepPromise { emptyList() } }

	override fun promiseAudioFiles(libraryId: LibraryId, query: String): Promise<List<ServiceFile>> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { it?.promiseFiles(query).keepPromise { emptyList() } }
}
