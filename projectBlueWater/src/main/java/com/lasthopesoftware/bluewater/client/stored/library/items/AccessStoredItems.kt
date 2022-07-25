package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface AccessStoredItems {
	fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean>
	fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean): Promise<Unit>
	fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean): Promise<Unit>
    fun isItemMarkedForSync(libraryId: LibraryId, item: IItem): Promise<Boolean>
    fun isItemMarkedForSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean>
	fun promiseStoredItems(libraryId: LibraryId): Promise<Collection<StoredItem>>
	fun disableAllLibraryItems(libraryId: LibraryId): Promise<Unit>
}
