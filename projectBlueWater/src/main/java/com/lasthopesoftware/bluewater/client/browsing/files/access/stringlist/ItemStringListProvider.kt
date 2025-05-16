package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class ItemStringListProvider(
	private val libraryConnections: ProvideLibraryConnections
) : ProvideFileStringListForItem {
	override fun promiseFileStringList(libraryId: LibraryId, itemId: ItemId?): Promise<String> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { a -> a?.promiseFileStringList(itemId).keepPromise("") }

	override fun promiseFileStringList(libraryId: LibraryId, playlistId: PlaylistId): Promise<String> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { a -> a?.promiseFileStringList(playlistId).keepPromise("") }

	override fun promiseShuffledFileStringList(libraryId: LibraryId, itemId: ItemId?): Promise<String> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { a -> a?.promiseShuffledFileStringList(itemId).keepPromise("") }

	override fun promiseShuffledFileStringList(libraryId: LibraryId, playlistId: PlaylistId): Promise<String> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { a -> a?.promiseShuffledFileStringList(playlistId).keepPromise("") }
}
