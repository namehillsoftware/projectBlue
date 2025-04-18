package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.sync.CollectServiceFilesForSync
import com.lasthopesoftware.policies.ExecutionPolicies

class DelegatingStoredItemServiceFileCollector(inner: CollectServiceFilesForSync, policies: ExecutionPolicies) : CollectServiceFilesForSync {
	private val serviceFilesForSyncPromise = policies.applyPolicy(inner::promiseServiceFilesToSync)

	override fun promiseServiceFilesToSync(libraryId: LibraryId) = serviceFilesForSyncPromise(libraryId)
}
