package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise

interface ManageConnectionSessions : ProvideLibraryConnections {
	fun promiseIsConnectionActive(libraryId: LibraryId): Promise<Boolean>

	fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>

	fun removeConnection(libraryId: LibraryId)
}
