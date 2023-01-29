package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise

interface ControlConnectionInitialization {

	fun promiseInitializedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, Boolean>
}
