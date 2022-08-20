package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise

class FakeLibraryConnectionProvider(private val connectionProviderMap: Map<LibraryId, IConnectionProvider>) :
    ProvideLibraryConnections {
    override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> {
        return ProgressingPromise(
            connectionProviderMap[libraryId]
        )
    }
}
