package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemUpdated

class StoredItemUpdatedListener(private val accessStoredItems: AccessStoredItems) : (ItemUpdated) -> Unit {
	override fun invoke(p1: ItemUpdated) {
		val (libraryId, item) = p1
		accessStoredItems.updateStoredItemMetadata(libraryId, item)
	}
}
