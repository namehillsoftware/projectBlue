package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.namehillsoftware.handoff.promises.Promise

class CachedItemProvider(
	private val inner: ProvideItems,
	private val revisions: CheckRevisions,
	private val itemFunctionCache: CachePromiseFunctions<Triple<LibraryId, ItemId?, Long>, List<IItem>> = companionCache,
) : ProvideItems {

	companion object {
		private val companionCache = LruPromiseCache<Triple<LibraryId, ItemId?, Long>, List<IItem>>(20)
	}

	override fun promiseItems(libraryId: LibraryId, itemId: ItemId?): Promise<List<IItem>> =
		revisions
			.promiseRevision(libraryId)
			.eventually { revision ->
				itemFunctionCache.getOrAdd(Triple(libraryId, itemId, revision)) { (l, k, _) -> inner.promiseItems(l, k) }
			}
}
