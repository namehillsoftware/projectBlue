package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class LibraryConnectionProvider : ProvideLibraryConnections {

	private val cachedConnectionProviders = ConcurrentHashMap<Int, IConnectionProvider>()
	private val promisedConnectionProvidersCache = HashMap<Int, Promise<IConnectionProvider>>()

	override fun buildLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}
