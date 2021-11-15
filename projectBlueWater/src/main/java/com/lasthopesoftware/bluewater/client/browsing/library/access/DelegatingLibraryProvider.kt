package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.policies.ApplyExecutionPolicies

class DelegatingLibraryProvider(inner: ILibraryProvider, policies: ApplyExecutionPolicies) : ILibraryProvider by inner {
	private val getLibraryFunc = policies.applyPolicy(inner::getLibrary)

	override fun getLibrary(libraryId: LibraryId) = getLibraryFunc(libraryId)
}
