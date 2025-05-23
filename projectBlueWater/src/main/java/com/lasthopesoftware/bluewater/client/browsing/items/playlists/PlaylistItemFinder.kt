package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.KnownViews
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class PlaylistItemFinder(private val itemProvider: ProvideItems) : FindPlaylistItem {
	override fun promiseItem(libraryId: LibraryId, playlist: Playlist): Promise<Item?> =
		itemProvider.promiseItems(libraryId)
			.eventually { v ->
				val playlistItem = v.single { i -> KnownViews.Playlists == i.value }
				recursivelySearchForPlaylist(libraryId, playlistItem, playlist)
			}

	private fun recursivelySearchForPlaylist(libraryId: LibraryId, rootItem: IItem, playlist: Playlist): Promise<Item?> =
		itemProvider.promiseItems(libraryId, ItemId(rootItem.key))
			.eventually { items ->
				val possiblePlaylistItem = items.filterIsInstance<Item>().firstOrNull { it.playlistId?.id == playlist.key }
				possiblePlaylistItem?.toPromise()
					?: Promise.whenAll(items.map { recursivelySearchForPlaylist(libraryId, it, playlist) })
						.then { aggregatedItems -> aggregatedItems.firstOrNull { it != null } }
			}
}
