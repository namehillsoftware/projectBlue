package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise

interface HoldConnections {
	fun isConnectionActive(libraryId: LibraryId): Boolean

	fun setAndGetPromisedConnection(libraryId: LibraryId, updater: (LibraryId, ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>?) -> ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>

	fun removeConnection(libraryId: LibraryId): Promise<IConnectionProvider?>?
}
