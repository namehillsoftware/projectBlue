package com.lasthopesoftware.bluewater.client.browsing.items.list

import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.namehillsoftware.handoff.promises.Promise

class ItemPlayback(
	private val itemStringListProvider: ProvideFileStringListForItem,
	private val controlPlaybackService: ControlPlaybackService
) : PlaybackLibraryItems {
	override fun playItem(libraryId: LibraryId, itemId: ItemId): Promise<Unit> =
		itemStringListProvider
			.promiseFileStringList(libraryId, itemId)
			.then { it -> controlPlaybackService.startPlaylist(libraryId, it) }

	override fun playPlaylist(libraryId: LibraryId, playlistId: PlaylistId): Promise<Unit> =
		itemStringListProvider
			.promiseFileStringList(libraryId, playlistId)
			.then { it -> controlPlaybackService.startPlaylist(libraryId, it) }

	override fun playItemShuffled(libraryId: LibraryId, itemId: ItemId): Promise<Unit> =
		itemStringListProvider
			.promiseShuffledFileStringList(libraryId, itemId)
			.then { it -> controlPlaybackService.startPlaylist(libraryId, it) }

	override fun playPlaylistShuffled(libraryId: LibraryId, playlistId: PlaylistId): Promise<Unit> =
		itemStringListProvider
			.promiseShuffledFileStringList(libraryId, playlistId)
			.then { it -> controlPlaybackService.startPlaylist(libraryId, it) }
}
