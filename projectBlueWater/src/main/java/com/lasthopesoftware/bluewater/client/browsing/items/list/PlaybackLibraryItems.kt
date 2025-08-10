package com.lasthopesoftware.bluewater.client.browsing.items.list

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface PlaybackLibraryItems {
	fun playItem(libraryId: LibraryId, itemId: ItemId, position: Int = 0): Promise<Unit>
	fun playPlaylist(libraryId: LibraryId, playlistId: PlaylistId, position: Int = 0): Promise<Unit>

	fun playItemShuffled(libraryId: LibraryId, itemId: ItemId): Promise<Unit>
	fun playPlaylistShuffled(libraryId: LibraryId, playlistId: PlaylistId): Promise<Unit>
}
