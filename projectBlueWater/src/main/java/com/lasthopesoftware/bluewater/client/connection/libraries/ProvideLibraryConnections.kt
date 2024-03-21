package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.promises.extensions.ProgressingPromise

interface ProvideLibraryConnections {
	fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>
}
