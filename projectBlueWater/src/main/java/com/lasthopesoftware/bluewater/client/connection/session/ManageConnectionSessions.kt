package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise

interface ManageConnectionSessions : ProvideLibraryConnections {
	fun promiseIsConnectionActive(libraryId: LibraryId): Promise<Boolean>

	fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>

	fun removeConnection(libraryId: LibraryId)
}
