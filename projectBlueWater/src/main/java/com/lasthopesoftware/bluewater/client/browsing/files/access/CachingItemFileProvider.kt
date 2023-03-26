package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.shared.policies.caching.CachePromiseFunctions
import com.namehillsoftware.handoff.promises.Promise

class CachingItemFileProvider(
	private val inner: ProvideItemFiles,
	private val revisions: CheckRevisions,
	private val itemFunctionCache: CachePromiseFunctions<Pair<Triple<LibraryId, ItemId?, FileListParameters.Options>, Int>, List<ServiceFile>>,
) : ProvideItemFiles {
	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId?, options: FileListParameters.Options): Promise<List<ServiceFile>> =
		revisions
			.promiseRevision(libraryId)
			.eventually { revision ->
				itemFunctionCache.getOrAdd(Pair(Triple(libraryId, itemId, options), revision)) { inner.promiseFiles(libraryId, itemId, options) }
			}
}
