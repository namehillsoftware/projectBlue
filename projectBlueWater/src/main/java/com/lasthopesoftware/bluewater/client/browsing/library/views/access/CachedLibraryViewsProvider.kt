package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.shared.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.namehillsoftware.handoff.promises.Promise

class CachedLibraryViewsProvider(
	private val inner: ProvideLibraryViews,
	private val revisions: CheckRevisions,
	private val functionCache: CachePromiseFunctions<Pair<LibraryId, Int>, Collection<ViewItem>>
) : ProvideLibraryViews {

	companion object {

		private val functionCache = LruPromiseCache<Pair<LibraryId, Int>, Collection<ViewItem>>(10)

		fun getInstance(context: Context): CachedLibraryViewsProvider {
			val libraryConnectionProvider = ConnectionSessionManager.get(context)

			return CachedLibraryViewsProvider(
				LibraryViewsProvider(libraryConnectionProvider),
				LibraryRevisionProvider(libraryConnectionProvider),
				functionCache
			)
		}
	}

	override fun promiseLibraryViews(libraryId: LibraryId): Promise<Collection<ViewItem>> =
		revisions
			.promiseRevision(libraryId)
			.eventually { revision ->
				functionCache.getOrAdd(Pair(libraryId, revision)) { (l, _) -> inner.promiseLibraryViews(l) }
			}
}
