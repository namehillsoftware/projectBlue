package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromiseProxy
import org.joda.time.Duration

private val dramaticPause by lazy { Duration.standardSeconds(2).plus(Duration.millis(500)) }

/*
Dramatically pauses before forwarding the result of getting the connection. If the connection was already retrieved,
there will be no dramatic pause.
 */
class DramaticConnectionInitializationProxy(
	private val manageConnectionSessions: ManageConnectionSessions,
) : ControlConnectionInitialization {

	@Synchronized
	override fun promiseInitializedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>() {
			init {
				val promisedConnection = manageConnectionSessions.promiseLibraryConnection(libraryId)
				val isConnectionAlreadyActive = manageConnectionSessions.isConnectionActive(libraryId)
				if (isConnectionAlreadyActive) {
					proxySuccess(promisedConnection)
				} else {
					proxyUpdates(promisedConnection)
					doCancel(promisedConnection)
					PromiseDelay
						.delay<Any?>(dramaticPause)
						.also(::doCancel)
						.must {
							proxy(promisedConnection)
						}
				}
			}
		}
}
