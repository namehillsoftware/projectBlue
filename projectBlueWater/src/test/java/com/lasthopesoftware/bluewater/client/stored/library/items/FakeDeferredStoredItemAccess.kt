package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise

abstract class FakeDeferredStoredItemAccess : AccessStoredItems {

	private var messenger: Messenger<Collection<StoredItem>>? = null

	fun resolveStoredItems() {
		messenger?.sendResolution(storedItems)
	}

	protected abstract val storedItems: Collection<StoredItem>

	override fun disableAllLibraryItems(libraryId: LibraryId): Promise<Unit> = Unit.toPromise()

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier) = true.toPromise()
	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean) = Unit.toPromise()
	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean) = Unit.toPromise()
	override fun isItemMarkedForSync(libraryId: LibraryId, item: IItem) = false.toPromise()

	override fun isItemMarkedForSync(libraryId: LibraryId, itemId: KeyedIdentifier) = false.toPromise()

	override fun promiseStoredItems(libraryId: LibraryId): Promise<Collection<StoredItem>> {
		return Promise { m -> messenger = m }
	}
}
