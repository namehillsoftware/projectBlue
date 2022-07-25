package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemHelpers.storedItemType
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class FakeStoredItemAccess(vararg initialStoredItems: StoredItem) : AccessStoredItems {

	private val inMemoryStoredItems: MutableList<StoredItem> = ArrayList()

	init {
		inMemoryStoredItems.addAll(listOf(*initialStoredItems))
	}

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean> {
		val type = itemId.storedItemType

		val matchingItems = findMatchingItems(itemId, type)
		val isSynced = matchingItems.any()
		if (isSynced) inMemoryStoredItems.removeAll(matchingItems)
		else inMemoryStoredItems.add(StoredItem(libraryId.id, itemId.id, type))

		return Promise(!isSynced)
	}

	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean): Promise<Unit> {
		if (enable) inMemoryStoredItems.add(
			StoredItem(
				libraryId.id,
				item.key,
				StoredItemHelpers.getListType(item)
			)
		) else inMemoryStoredItems.removeAll(findMatchingItems(item))
		return Unit.toPromise()
	}

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean): Promise<Unit> {
		val type = itemId.storedItemType

		if (enable) inMemoryStoredItems.add(StoredItem(libraryId.id, itemId.id, type))
		else inMemoryStoredItems.removeAll(findMatchingItems(itemId, type))

		return Unit.toPromise()
	}

	override fun isItemMarkedForSync(libraryId: LibraryId, item: IItem): Promise<Boolean> {
		return Promise(findMatchingItems(item).isNotEmpty())
	}

	override fun isItemMarkedForSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean> {
		return Promise(findMatchingItems(itemId, itemId.storedItemType).any())
	}

	override fun promiseStoredItems(libraryId: LibraryId): Promise<Collection<StoredItem>> {
		return Promise(inMemoryStoredItems.toList())
	}

	private fun findMatchingItems(item: IItem): List<StoredItem> {
		return inMemoryStoredItems
			.filter { i -> i.serviceId == item.key && i.itemType === StoredItemHelpers.getListType(item) }
			.toList()
	}

	private fun findMatchingItems(item: KeyedIdentifier, type: StoredItem.ItemType): List<StoredItem> {
		return inMemoryStoredItems
			.filter { i -> i.serviceId == item.id && i.itemType === type }
			.toList()
	}

	override fun disableAllLibraryItems(libraryId: LibraryId): Promise<Unit> {
		inMemoryStoredItems.removeAll(inMemoryStoredItems.filter { s -> s.libraryId == libraryId.id })
		return Unit.toPromise()
	}
}
