package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise

interface HoldPromisedConnections {
	fun getPromisedResolvedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>?

	fun setAndGetPromisedConnection(libraryId: LibraryId, updater: (LibraryId, ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>?) -> ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>

	fun removeConnection(libraryId: LibraryId): Promise<ProvideConnections?>?
}
