package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.library.views.KnownViews
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.ProvideLibraryViews
import com.namehillsoftware.handoff.promises.Promise

class PlaylistItemFinder(private val libraryViews: ProvideLibraryViews, private val itemProvider: ProvideItems) : FindPlaylistItem {
	override fun promiseItem(playlist: Playlist): Promise<Item?> {
		return libraryViews.promiseLibraryViews()
			.eventually<Item> { v ->
				val playlistItem = v.single { i -> KnownViews.Playlists == i.value }
				recursivelySearchForPlaylist(playlistItem, playlist)
			}
	}

	private fun recursivelySearchForPlaylist(rootItem: Item, playlist: Playlist): Promise<Item?> {
		return itemProvider.promiseItems(rootItem.key)
			.eventually { items ->
				val possiblePlaylistItem = items.firstOrNull { i -> i.playlistId != null && i.playlistId == playlist.key }
				if (possiblePlaylistItem != null) Promise(possiblePlaylistItem)
				else Promise.whenAll(items.map { recursivelySearchForPlaylist(it, playlist) })
						.then { aggregatedItems ->
							aggregatedItems.firstOrNull { it != null }
						}
			}
	}
}
