package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.LiveServerConnection
import com.lasthopesoftware.promises.ResolvedPromiseBox
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise

class PromisedConnectionsRepository : HoldPromisedConnections {
	private val promisedConnectionProvidersCache = HashMap<LibraryId, ResolvedPromiseBox<LiveServerConnection?, ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>>>()
	private val sessionSync = Any()

	override fun getPromisedResolvedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>? =
		promisedConnectionProvidersCache[libraryId]?.resolvedPromise

	override fun setAndGetPromisedConnection(libraryId: LibraryId, updater: (LibraryId, ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>?) -> ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?> =
		synchronized(sessionSync) {
			updater(libraryId, promisedConnectionProvidersCache[libraryId]?.originalPromise)
				.also { promisedConnectionProvidersCache[libraryId] = ResolvedPromiseBox(it) }
		}

	override fun removeConnection(libraryId: LibraryId): Promise<LiveServerConnection?>? =
		synchronized(sessionSync) {
			promisedConnectionProvidersCache.remove(libraryId)?.originalPromise
		}
}
