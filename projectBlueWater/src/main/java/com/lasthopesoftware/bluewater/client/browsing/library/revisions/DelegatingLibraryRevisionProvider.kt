package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.policies.ExecutionPolicies
import com.namehillsoftware.handoff.promises.Promise

class DelegatingLibraryRevisionProvider(private val inner: CheckRevisions, policies: ExecutionPolicies) : CheckRevisions {
	private val revisionFunc by lazy { policies.applyPolicy(inner::promiseRevision) }

	override fun promiseRevision(libraryId: LibraryId): Promise<Long> = revisionFunc(libraryId)
}
