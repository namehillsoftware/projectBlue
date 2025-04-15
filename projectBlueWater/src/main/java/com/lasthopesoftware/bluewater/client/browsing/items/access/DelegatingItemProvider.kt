package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.policies.ApplyExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

class DelegatingItemProvider(private val inner: ProvideItems, policies: ApplyExecutionPolicies) : ProvideItems {
	private val promiseItemsDelegate = policies.applyPolicy<LibraryId, ItemId?, List<IItem>>(inner::promiseItems)

	override fun promiseItems(libraryId: LibraryId, itemId: ItemId?): Promise<List<IItem>> =
		promiseItemsDelegate(libraryId, itemId)
}
