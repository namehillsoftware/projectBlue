package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.promises.extensions.ProgressingPromise

class FakeLibraryConnectionProvider(private val connectionProviderMap: Map<LibraryId, LiveServerConnection>) :
    ProvideLibraryConnections {
    override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?> {
        return ProgressingPromise(
            connectionProviderMap[libraryId]
        )
    }
}
