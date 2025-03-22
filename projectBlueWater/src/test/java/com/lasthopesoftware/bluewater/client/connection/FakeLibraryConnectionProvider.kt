package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideProgressingLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.promises.extensions.ProgressingPromise

class FakeLibraryConnectionProvider(private val connectionProviderMap: Map<LibraryId, LiveServerConnection>) :
	ProvideProgressingLibraryConnections {
    override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?> {
        return ProgressingPromise(
            connectionProviderMap[libraryId]
        )
    }
}
