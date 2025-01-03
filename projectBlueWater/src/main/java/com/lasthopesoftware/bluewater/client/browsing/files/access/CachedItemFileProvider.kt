package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.namehillsoftware.handoff.promises.Promise

class CachedItemFileProvider(
	private val inner: ProvideItemFiles,
	private val revisions: CheckRevisions,
	private val itemFunctionCache: CachePromiseFunctions<Pair<Triple<LibraryId, ItemId?, FileListParameters.Options>, Int>, List<ServiceFile>> = companionCache,
) : ProvideItemFiles {

	companion object {
		private val companionCache = LruPromiseCache<Pair<Triple<LibraryId, ItemId?, FileListParameters.Options>, Int>, List<ServiceFile>>(10)
	}

	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId?, options: FileListParameters.Options): Promise<List<ServiceFile>> =
		revisions
			.promiseRevision(libraryId)
			.eventually { revision ->
				itemFunctionCache.getOrAdd(Pair(Triple(libraryId, itemId, options), revision)) { inner.promiseFiles(libraryId, itemId, options) }
			}
}
