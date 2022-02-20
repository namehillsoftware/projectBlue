package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class FakeStoredItemAccess(vararg initialStoredItems: StoredItem) : AccessStoredItems {

	private val inMemoryStoredItems: MutableList<StoredItem> = ArrayList()

	init {
		inMemoryStoredItems.addAll(listOf(*initialStoredItems))
	}

	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean) {
		if (enable) inMemoryStoredItems.add(
			StoredItem(
				libraryId.id,
				item.key,
				StoredItemHelpers.getListType(item)
			)
		) else inMemoryStoredItems.removeAll(findMatchingItems(item))
	}

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean) {
		TODO("Not yet implemented")
	}

	override fun isItemMarkedForSync(libraryId: LibraryId, item: IItem): Promise<Boolean> {
		return Promise(findMatchingItems(item).isNotEmpty())
	}

	override fun promiseStoredItems(libraryId: LibraryId): Promise<Collection<StoredItem>> {
		return Promise(inMemoryStoredItems.toList())
	}

	private fun findMatchingItems(item: IItem): List<StoredItem> {
		return inMemoryStoredItems
			.filter { i -> i.serviceId == item.key && i.itemType === StoredItemHelpers.getListType(item) }
			.toList()
	}

	override fun disableAllLibraryItems(libraryId: LibraryId): Promise<Unit> {
		inMemoryStoredItems.removeAll(inMemoryStoredItems.filter { s -> s.libraryId == libraryId.id })
		return Unit.toPromise()
	}
}
