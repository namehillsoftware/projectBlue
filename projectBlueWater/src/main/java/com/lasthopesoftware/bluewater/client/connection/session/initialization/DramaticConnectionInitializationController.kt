package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.joda.time.Duration

private val dramaticPause by lazy { Duration.standardSeconds(2).plus(Duration.millis(500)) }
private val logger by lazyLogger<DramaticConnectionInitializationController>()

/*
Dramatically pauses before forwarding the result of getting the connection. If the connection was already retrieved,
there will be no dramatic pause.
 */
class DramaticConnectionInitializationController(
	private val manageConnectionSessions: ManageConnectionSessions,
	private val applicationNavigation: NavigateApplication,
) : ControlConnectionInitialization {

	@Synchronized
	override fun promiseActiveLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>() {
			init {
				manageConnectionSessions
					.promiseIsConnectionActive(libraryId)
					.eventually { isConnectionAlreadyActive ->
						if (isConnectionAlreadyActive) {
							manageConnectionSessions.promiseLibraryConnection(libraryId)
						} else {
							val promisedConnection = manageConnectionSessions.promiseTestedLibraryConnection(libraryId)
							proxyUpdates(promisedConnection)
							doCancel(promisedConnection)
							promisedConnection
								.inevitably {
									PromiseDelay
										.delay<Any?>(dramaticPause)
										.also(::doCancel)
								}
						}
					}
					.eventually(
						{ c ->
							if (c == null) {
								manageConnectionSessions.removeConnection(libraryId)
								applicationNavigation
									.viewApplicationSettings()
									.also(::doCancel)
									.then({ null }, { null })
							} else {
								c.toPromise()
							}
						},
						{  e ->
							logger.error("An error occurred getting the connection for library ID ${libraryId.id}.", e)
							applicationNavigation.viewApplicationSettings().then({ null }, { null })
						}
					)
					.then(::resolve, ::reject)
			}
		}
}