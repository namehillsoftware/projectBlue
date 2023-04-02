package com.lasthopesoftware.bluewater.client.browsing.items.list

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface PlaybackLibraryItems {
	fun playItem(libraryId: LibraryId, itemId: ItemId): Promise<Unit>

	fun playItemShuffled(libraryId: LibraryId, itemId: ItemId): Promise<Unit>
}
