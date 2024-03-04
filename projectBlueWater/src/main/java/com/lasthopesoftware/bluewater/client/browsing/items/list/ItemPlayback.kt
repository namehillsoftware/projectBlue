package com.lasthopesoftware.bluewater.client.browsing.items.list

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.namehillsoftware.handoff.promises.Promise

class ItemPlayback(
	private val itemStringListProvider: ProvideFileStringListForItem,
	private val controlPlaybackService: ControlPlaybackService
) : PlaybackLibraryItems {
	override fun playItem(libraryId: LibraryId, itemId: ItemId): Promise<Unit> =
		itemStringListProvider
			.promiseFileStringList(libraryId, itemId, FileListParameters.Options.None)
			.then { it -> controlPlaybackService.startPlaylist(libraryId, it) }

	override fun playItemShuffled(libraryId: LibraryId, itemId: ItemId): Promise<Unit> =
		itemStringListProvider
			.promiseFileStringList(libraryId, itemId, FileListParameters.Options.Shuffled)
			.then { it -> controlPlaybackService.startPlaylist(libraryId, it) }
}
