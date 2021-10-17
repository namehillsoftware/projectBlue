package com.lasthopesoftware.bluewater.client.browsing.items.access

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.shared.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.namehillsoftware.handoff.promises.Promise

class CachedItemProvider(
	private val inner: ProvideItems,
	private val revisions: CheckRevisions,
	private val functionCache: CachePromiseFunctions<Triple<LibraryId, Int, Int>, List<Item>>
) : ProvideItems {

	companion object {

		private val functionCache = LruPromiseCache<Triple<LibraryId, Int, Int>, List<Item>>(20)

		fun getInstance(context: Context): CachedItemProvider {
			val libraryConnectionProvider = ConnectionSessionManager.get(context)

			return CachedItemProvider(
				ItemProvider(libraryConnectionProvider),
				LibraryRevisionProvider(libraryConnectionProvider),
				functionCache
			)
		}
	}

	override fun promiseItems(libraryId: LibraryId, itemKey: Int): Promise<List<Item>> =
		revisions
			.promiseRevision(libraryId)
			.eventually { revision ->
				functionCache.getOrAdd(Triple(libraryId, itemKey, revision)) { (l, k, _) -> inner.promiseItems(l, k) }
			}
}
