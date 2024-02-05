package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.TrackedConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.bluewater.shared.promises.extensions.guaranteedUnitResponse
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class TrackedLibraryConnectionSessions(private val inner: ManageConnectionSessions) : ManageConnectionSessions by inner, PromisingCloseable {

	private val libraryConnections = ConcurrentHashMap<LibraryId, ConcurrentHashMap<IConnectionProvider, TrackedConnectionProvider>>()

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> {
		return object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>() {
			init {
				inner
					.promiseLibraryConnection(libraryId)
					.also(::proxyUpdates)
					.also(::proxyRejection)
					.then { connection ->
						resolve(connection?.let { cp -> getTrackedConnection(libraryId, cp) })
					}
			}
		}
	}

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> {
		return object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>() {
			init {
				inner
					.promiseTestedLibraryConnection(libraryId)
					.also(::proxyUpdates)
					.also(::proxyRejection)
					.then { connection ->
						resolve(connection?.let { cp -> getTrackedConnection(libraryId, cp) })
					}
			}
		}
	}

	override fun removeConnection(libraryId: LibraryId) {
		inner.removeConnection(libraryId)
		libraryConnections.remove(libraryId)
	}

	override fun promiseClose(): Promise<Unit> = Promise
		.whenAll(libraryConnections.values.flatMap { it.values.map{ cp -> cp.promiseClose() } })
		.guaranteedUnitResponse()
		.must { libraryConnections.clear() }

	private fun getTrackedConnection(libraryId: LibraryId, sourceConnection: IConnectionProvider): TrackedConnectionProvider {
		val existingConnections = libraryConnections.getOrPut(libraryId) { ConcurrentHashMap() }
		val trackedConnection = existingConnections.getOrPut(sourceConnection) { TrackedConnectionProvider(sourceConnection) }

		if (existingConnections.size == 1) return trackedConnection

		for ((existingConnection, existingTrackedConnection) in existingConnections) {
			if (existingConnection == sourceConnection && existingTrackedConnection == trackedConnection) continue

			existingTrackedConnection
				.promiseClose()
				.must { existingConnections.remove(existingConnection, existingTrackedConnection) }
		}

		return trackedConnection
	}
}
