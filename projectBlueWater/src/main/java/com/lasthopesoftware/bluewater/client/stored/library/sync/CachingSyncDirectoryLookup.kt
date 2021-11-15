package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.policies.ApplyExecutionPolicies

class CachingSyncDirectoryLookup(inner: LookupSyncDirectory, policies: ApplyExecutionPolicies) : LookupSyncDirectory {
	private val syncDirectoryPromise = policies.applyPolicy(inner::promiseSyncDirectory)

	override fun promiseSyncDirectory(libraryId: LibraryId) = syncDirectoryPromise(libraryId)
}
