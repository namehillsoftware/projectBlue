package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class ConnectionAuthenticationChecker(private val libraryConnections: ProvideLibraryConnections) : CheckIfConnectionIsReadOnly {
	override fun promiseIsReadOnly(libraryId: LibraryId): Promise<Boolean> = libraryConnections
		.promiseLibraryConnection(libraryId)
		.eventuallyFromDataAccess { it?.promiseIsReadOnly().keepPromise(false) }
}
