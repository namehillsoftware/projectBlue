package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface FindPlaylistItem {
	fun promiseItem(libraryId: LibraryId, playlist: Playlist): Promise<Item?>
}
