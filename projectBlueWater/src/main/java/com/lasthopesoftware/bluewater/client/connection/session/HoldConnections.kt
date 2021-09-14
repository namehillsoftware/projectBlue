package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise

interface HoldConnections {
	fun isConnectionActive(libraryId: LibraryId): Boolean

	fun getOrAddPromisedConnection(libraryId: LibraryId, factory: (LibraryId) -> ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>
}
