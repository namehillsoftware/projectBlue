package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import java.util.concurrent.ConcurrentHashMap

object ConnectionRepository : HoldConnections {
	private val cachedActiveConnections = ConcurrentHashMap<LibraryId, Unit>()
	private val promisedConnectionProvidersCache = ConcurrentHashMap<LibraryId, ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>>()
	private val sessionSync = Any()

	override fun isConnectionActive(libraryId: LibraryId): Boolean = cachedActiveConnections.containsKey(libraryId)

	override fun getOrAddPromisedConnection(libraryId: LibraryId, factory: (LibraryId) -> ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> {
		return promisedConnectionProvidersCache[libraryId] ?:
			synchronized(sessionSync) {
				promisedConnectionProvidersCache[libraryId]
					?: factory(libraryId).also {
						promisedConnectionProvidersCache[libraryId] = it
						it.then { c ->
							if (c != null) cachedActiveConnections[libraryId] = Unit
							else cachedActiveConnections.remove(libraryId)
						}
					}
			}
	}
}
