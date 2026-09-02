package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemHelpers.storedItemType
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class FakeStoredItemAccess(vararg initialStoredItems: StoredItem) : AccessStoredItems {

	val inMemoryStoredItems = mutableListOf<StoredItem>()

	init {
		inMemoryStoredItems.addAll(listOf(*initialStoredItems))
	}

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean> {
		val type = itemId.storedItemType

		val matchingItems = findMatchingItems(libraryId, itemId, type)
		val isSynced = matchingItems.any()
		if (isSynced) inMemoryStoredItems.removeAll(matchingItems)
		else inMemoryStoredItems.add(StoredItem(libraryId.id, itemId.id, type))

		return Promise(!isSynced)
	}

	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean): Promise<Unit> {
		val item = inferItem(item)
		if (enable) inMemoryStoredItems.add(
			StoredItem(
				libraryId.id,
				item.key,
				item.storedItemType,
				item.value,
			)
		) else inMemoryStoredItems.removeAll(findMatchingItems(libraryId, item))
		return Unit.toPromise()
	}

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean): Promise<Unit> {
		val type = itemId.storedItemType

		if (enable) inMemoryStoredItems.add(StoredItem(libraryId.id, itemId.id, type))
		else inMemoryStoredItems.removeAll(findMatchingItems(libraryId, itemId, type))

		return Unit.toPromise()
	}

	override fun isItemMarkedForSync(libraryId: LibraryId, item: IItem): Promise<Boolean> {
		return Promise(findMatchingItems(libraryId, item).isNotEmpty())
	}

	override fun isItemMarkedForSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean> {
		return Promise(findMatchingItems(libraryId, itemId, itemId.storedItemType).isNotEmpty())
	}

	override fun promiseStoredItems(libraryId: LibraryId): Promise<Collection<StoredItem>> {
		return Promise(inMemoryStoredItems.filter { s -> s.libraryId == libraryId.id })
	}

	override fun disableAllLibraryItems(libraryId: LibraryId): Promise<Unit> {
		inMemoryStoredItems.removeAll(inMemoryStoredItems.filter { s -> s.libraryId == libraryId.id })
		return Unit.toPromise()
	}

	override fun updateStoredItemMetadata(libraryId: LibraryId, item: IItem): Promise<Unit> {
		val storedItems = findMatchingItems(libraryId, item)
		for (storedItem in storedItems)
			storedItem.itemName = item.value
		return Unit.toPromise()
	}

	private fun findMatchingItems(libraryId: LibraryId, item: IItem): List<StoredItem> {
		return inMemoryStoredItems
			.filter { i -> i.libraryId == libraryId.id && i.serviceId == item.key && i.itemType === item.storedItemType }
	}

	private fun findMatchingItems(libraryId: LibraryId, item: KeyedIdentifier, type: StoredItem.ItemType): List<StoredItem> {
		return inMemoryStoredItems
			.filter { i -> i.libraryId == libraryId.id && i.serviceId == item.id && i.itemType === type }
	}

	private fun inferItem(item: IItem): IItem {
		if (item is Item) {
			val playlist = item.playlistId
			if (playlist != null) return Playlist(playlist.id, item.value)
		}
		return item
	}
}
