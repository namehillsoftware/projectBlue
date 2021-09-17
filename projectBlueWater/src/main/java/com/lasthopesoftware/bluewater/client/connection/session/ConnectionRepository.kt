package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class ConnectionRepository : HoldConnections {
	private val cachedActiveConnections = ConcurrentHashMap<LibraryId, Unit>()
	private val promisedConnectionProvidersCache = HashMap<LibraryId, ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>>()
	private val sessionSync = Any()

	override fun isConnectionActive(libraryId: LibraryId): Boolean = cachedActiveConnections.containsKey(libraryId)

	override fun setAndGetPromisedConnection(libraryId: LibraryId, updater: (LibraryId, ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>?) -> ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		synchronized(sessionSync) {
			updater(libraryId, promisedConnectionProvidersCache[libraryId])
				.also {
					promisedConnectionProvidersCache[libraryId] = it
					it.then { c ->
						if (c != null) cachedActiveConnections[libraryId] = Unit
						else cachedActiveConnections.remove(libraryId)
					}
				}
		}

	override fun removeConnection(libraryId: LibraryId): Promise<IConnectionProvider?>? =
		synchronized(sessionSync) {
			cachedActiveConnections.remove(libraryId)
			promisedConnectionProvidersCache.remove(libraryId)
		}
}
