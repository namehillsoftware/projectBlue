package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.policies.ExecutionPolicies

class DelegatingLibraryProvider(inner: ProvideLibraries, policies: ExecutionPolicies) : ProvideLibraries by inner {
	private val getLibraryFunc = policies.applyPolicy(inner::promiseLibrary)
	private val getNowPlayingFunc = policies.applyPolicy(inner::promiseNowPlayingValues)

	override fun promiseLibrary(libraryId: LibraryId) = getLibraryFunc(libraryId)
	override fun promiseNowPlayingValues(libraryId: LibraryId) = getNowPlayingFunc(libraryId)
}
