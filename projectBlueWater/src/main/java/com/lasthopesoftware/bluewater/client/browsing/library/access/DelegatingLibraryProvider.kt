package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.policies.ApplyExecutionPolicies

class DelegatingLibraryProvider(inner: ILibraryProvider, policies: ApplyExecutionPolicies) : ILibraryProvider by inner {
	private val getLibraryFunc = policies.applyPolicy(inner::promiseLibrary)

	override fun promiseLibrary(libraryId: LibraryId) = getLibraryFunc(libraryId)
}
