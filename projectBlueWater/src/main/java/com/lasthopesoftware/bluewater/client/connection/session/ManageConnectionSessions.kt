package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise

interface ManageConnectionSessions {
	fun promiseIsConnectionActive(libraryId: LibraryId): Promise<Boolean>

	fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>

	fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>

	fun removeConnection(libraryId: LibraryId)
}
