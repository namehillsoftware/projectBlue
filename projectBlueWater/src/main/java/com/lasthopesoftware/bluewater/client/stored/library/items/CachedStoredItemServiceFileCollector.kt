package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.sync.CollectServiceFilesForSync
import com.lasthopesoftware.bluewater.shared.policies.caching.CachingPolicyFactory

class CachedStoredItemServiceFileCollector(inner: CollectServiceFilesForSync, cachePolicyFactory: CachingPolicyFactory) : CollectServiceFilesForSync {
	private val serviceFilesForSyncPromise = cachePolicyFactory.applyPolicy(inner::promiseServiceFilesToSync)

	override fun promiseServiceFilesToSync(libraryId: LibraryId) = serviceFilesForSyncPromise(libraryId)
}
