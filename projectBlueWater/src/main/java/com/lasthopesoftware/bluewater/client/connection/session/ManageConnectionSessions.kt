package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise

interface ManageConnectionSessions : ProvideLibraryConnections {
	fun isConnectionActive(libraryId: LibraryId): Boolean

	fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>
}
