package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class CachedSelectedLibraryIdProvider(
	private val inner: ProvideSelectedLibraryId,
	private val selectedLibraryIdCache: HoldSelectedLibraryId,
) : ProvideSelectedLibraryId
{

	override fun promiseSelectedLibraryId(): Promise<LibraryId?> = selectedLibraryIdCache.getOrCache(inner::promiseSelectedLibraryId)
}
