package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class StaticLibraryIdentifierProvider constructor(instanceLibraryProvider: ProvideSelectedLibraryId) : ProvideSelectedLibraryId {
	private val promisedLibraryId = instanceLibraryProvider.promiseSelectedLibraryId()

	override fun promiseSelectedLibraryId(): Promise<LibraryId?> = promisedLibraryId
}
