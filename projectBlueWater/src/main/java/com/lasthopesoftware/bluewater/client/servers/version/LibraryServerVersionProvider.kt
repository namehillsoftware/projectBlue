package com.lasthopesoftware.bluewater.client.servers.version

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryServerVersionProvider(private val libraryConnections: ProvideLibraryConnections) : ProvideLibraryServerVersion {
	override fun promiseServerVersion(libraryId: LibraryId): Promise<SemanticVersion?> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { it?.promiseServerVersion().keepPromise() }
}
