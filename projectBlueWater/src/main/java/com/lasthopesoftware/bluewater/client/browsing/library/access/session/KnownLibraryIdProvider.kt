package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class KnownLibraryIdProvider(private val libraryId: LibraryId) : ProvideSelectedLibraryId {
	private val cachedLibraryIdPromise by lazy { Promise(libraryId) }

	override fun promiseSelectedLibraryId(): Promise<LibraryId?> = cachedLibraryIdPromise
}
