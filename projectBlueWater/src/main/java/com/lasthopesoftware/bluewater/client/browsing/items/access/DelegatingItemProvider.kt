package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.policies.ApplyExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

class DelegatingItemProvider(private val inner: ProvideItems, policies: ApplyExecutionPolicies) : ProvideItems {
	private val promiseItemsDelegate = policies.applyPolicy<LibraryId, KeyedIdentifier?, List<IItem>>(inner::promiseItems)

	override fun promiseItems(libraryId: LibraryId, itemId: KeyedIdentifier?): Promise<List<IItem>> =
		promiseItemsDelegate(libraryId, itemId)
}
