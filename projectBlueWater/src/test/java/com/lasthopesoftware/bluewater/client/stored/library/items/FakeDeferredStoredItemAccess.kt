package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise

abstract class FakeDeferredStoredItemAccess : IStoredItemAccess {

	private var messenger: Messenger<Collection<StoredItem>>? = null

	fun resolveStoredItems() {
		messenger?.sendResolution(storedItems)
	}

	protected abstract val storedItems: Collection<StoredItem>

	override fun disableAllLibraryItems(libraryId: LibraryId): Promise<Unit> {
		return Unit.toPromise()
	}

	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean) {}
	override fun isItemMarkedForSync(libraryId: LibraryId, item: IItem): Promise<Boolean> {
		return Promise(false)
	}

	override fun promiseStoredItems(libraryId: LibraryId): Promise<Collection<StoredItem>> {
		return Promise { m -> messenger = m }
	}
}
