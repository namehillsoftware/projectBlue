package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideFileStringListForItem {
	fun promiseFileStringList(libraryId: LibraryId, itemId: ItemId? = null): Promise<String>
	fun promiseShuffledFileStringList(libraryId: LibraryId, itemId: ItemId? = null): Promise<String>
	fun promiseFileStringList(libraryId: LibraryId, playlistId: PlaylistId): Promise<String>
	fun promiseShuffledFileStringList(libraryId: LibraryId, playlistId: PlaylistId): Promise<String>
}
