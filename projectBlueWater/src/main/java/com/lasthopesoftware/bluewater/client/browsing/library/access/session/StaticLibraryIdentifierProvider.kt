package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

class StaticLibraryIdentifierProvider constructor(instanceLibraryProvider: ProvideSelectedLibraryId) : ProvideSelectedLibraryId {
	private val libraryId = instanceLibraryProvider.selectedLibraryId

	override val selectedLibraryId: LibraryId?
		get() = libraryId
}
