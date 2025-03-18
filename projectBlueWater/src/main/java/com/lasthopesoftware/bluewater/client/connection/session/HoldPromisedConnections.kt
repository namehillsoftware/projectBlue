package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise

interface HoldPromisedConnections {
	fun getPromisedResolvedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>?

	fun setAndGetPromisedConnection(libraryId: LibraryId, updater: (LibraryId, ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>?) -> ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>

	fun removeConnection(libraryId: LibraryId): Promise<LiveServerConnection?>?
}
