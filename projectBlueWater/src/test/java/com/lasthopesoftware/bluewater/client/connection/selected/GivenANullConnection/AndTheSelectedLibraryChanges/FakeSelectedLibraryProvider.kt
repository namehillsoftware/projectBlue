package com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class FakeSelectedLibraryProvider : ProvideSelectedLibraryId {
	var libraryId = LibraryId(0)

	override fun promiseSelectedLibraryId(): Promise<LibraryId?> = Promise(libraryId)
}
