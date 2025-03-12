package com.lasthopesoftware.bluewater.client.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise

class RemoteLibraryAccessProvider(private val provideLibraryConnections: ProvideLibraryConnections) :
	ProvideRemoteLibraryAccess
{
	override fun promiseLibraryAccess(libraryId: LibraryId): Promise<RemoteLibraryAccess?> =
		provideLibraryConnections.promiseLibraryConnection(libraryId).cancelBackThen { c, _ -> c?.getDataAccess() }
}
