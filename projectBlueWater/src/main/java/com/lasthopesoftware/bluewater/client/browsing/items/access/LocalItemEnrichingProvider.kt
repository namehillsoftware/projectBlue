package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.StoredLocalItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise

class LocalItemEnrichingProvider(
	private val inner: ProvideFreshItems,
	private val stringResources: GetStringResources
) :
	ProvideItems,
	ProvideFreshItems {
	override fun promiseItems(libraryId: LibraryId, itemId: KeyedIdentifier?): Promise<List<IItem>> {
		val promisedItems = inner.promiseItems(libraryId, itemId)
		return if (itemId != null) promisedItems
		else promisedItems.cancelBackThen { items, cs ->
			if (!cs.isCancelled) items + StoredLocalItem(stringResources.syncedItems) else items
		}
	}
}
