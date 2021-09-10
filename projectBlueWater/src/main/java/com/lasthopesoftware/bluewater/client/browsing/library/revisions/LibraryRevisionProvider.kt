package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.namehillsoftware.handoff.promises.Promise

class LibraryRevisionProvider(private val libraryConnections: ProvideLibraryConnections) : CheckRevisions {
	override fun promiseRevision(libraryId: LibraryId): Promise<Int> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually(RevisionStorage::promiseRevision)
}
