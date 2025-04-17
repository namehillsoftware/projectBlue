package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.namehillsoftware.handoff.promises.Promise

class CachedItemFileProvider(
	private val inner: ProvideItemFiles,
	private val revisions: CheckRevisions,
	private val itemFunctionCache: CachePromiseFunctions<Triple<LibraryId, ItemId?, Long>, List<ServiceFile>> = companionCache,
) : ProvideItemFiles {

	companion object {
		private val companionCache = LruPromiseCache<Triple<LibraryId, ItemId?, Long>, List<ServiceFile>>(10)
	}

	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId?): Promise<List<ServiceFile>> =
		revisions
			.promiseRevision(libraryId)
			.eventually { revision ->
				itemFunctionCache.getOrAdd(Triple(libraryId, itemId, revision)) { inner.promiseFiles(
                    libraryId,
                    itemId
                ) }
			}
}
