package com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class FakeSelectedLibraryIdProvider(
	var libraryId: LibraryId = LibraryId(0)
) : ProvideSelectedLibraryId {
	override fun promiseSelectedLibraryId(): Promise<LibraryId?> = Promise(libraryId)
}
