package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.shared.policies.caching.CachePromiseFunctions
import com.namehillsoftware.handoff.promises.Promise

class CachingItemFileProvider(
	private val inner: ProvideItemFiles,
	private val revisions: CheckRevisions,
	private val functionCache: CachePromiseFunctions<Pair<Triple<LibraryId, ItemId, FileListParameters.Options>, Int>, List<ServiceFile>>
) : ProvideItemFiles {
	override fun promiseFiles(libraryId: LibraryId, itemId: ItemId, options: FileListParameters.Options): Promise<List<ServiceFile>> =
		revisions
			.promiseRevision(libraryId)
			.eventually { revision ->
				functionCache.getOrAdd(Pair(Triple(libraryId, itemId, options), revision)) { inner.promiseFiles(libraryId, itemId, options) }
			}
}
