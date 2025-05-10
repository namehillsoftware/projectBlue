package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideProgressingLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.PromiseDelay
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import org.joda.time.Duration
import kotlin.coroutines.cancellation.CancellationException

private val dramaticPause by lazy { Duration.standardSeconds(2).plus(Duration.millis(500)) }
private val logger by lazyLogger<DramaticConnectionInitializationController>()

/*
Dramatically pauses before forwarding the result of getting the connection. If the connection was already retrieved,
there will be no dramatic pause.
 */
class DramaticConnectionInitializationController(
	private val manageConnectionSessions: ManageConnectionSessions,
) : ProvideProgressingLibraryConnections {

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, LiveServerConnection?>() {
			init {
				proxy(
					manageConnectionSessions
						.promiseIsConnectionActive(libraryId)
						.eventually { isConnectionAlreadyActive ->
							if (isConnectionAlreadyActive) {
								logger.debug("Connection for {} already active.", libraryId)
								manageConnectionSessions.promiseLibraryConnection(libraryId)
							} else {
								logger.debug("Connection for {} not active, creating.", libraryId)
								val promisedConnection = manageConnectionSessions.promiseTestedLibraryConnection(libraryId)
								proxyProgress(promisedConnection)
								doCancel(promisedConnection)
								promisedConnection
									.inevitably {
										PromiseDelay
											.delay<Any?>(dramaticPause)
											.also(::doCancel)
									}
							}
						}
						.then(
							{ c ->
								if (c == null)
									manageConnectionSessions.removeConnection(libraryId)

								c
							},
							{  e ->
								logger.error("An error occurred getting the connection for library ID ${libraryId.id}.", e)

								if (e !is CancellationException) manageConnectionSessions.removeConnection(libraryId)

								null
							}
						)
				)
			}
		}
}
