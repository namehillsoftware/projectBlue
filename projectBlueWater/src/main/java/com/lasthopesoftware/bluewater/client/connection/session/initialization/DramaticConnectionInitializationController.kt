package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.PromiseDelay
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.EventualAction
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
) : ManageConnectionSessions by manageConnectionSessions, EventualAction {

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, ProvideConnections?>() {
			init {
				val promisedConnection = manageConnectionSessions.promiseTestedLibraryConnection(libraryId)
				proxyUpdates(promisedConnection)
				doCancel(promisedConnection)
				proxy(
					promisedConnection
						.inevitably(this@DramaticConnectionInitializationController)
						.also(::doCancel)
						.eventually(
							{ c ->
								if (c == null) {
									removeConnection(libraryId)
									applicationNavigation
										.viewApplicationSettings()
										.also(::doCancel)
										.then({ _ -> null }, { null })
								} else {
									c.toPromise()
								}
							},
							{ e ->
								logger.error("An error occurred getting the connection for library ID ${libraryId.id}.", e)
								applicationNavigation.viewApplicationSettings().then({ _ -> null }, { null })
							}
						)
				)
			}
		}

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, ProvideConnections?>() {
			init {
				val promisedConnection = manageConnectionSessions
					.promiseIsConnectionActive(libraryId)
					.eventually { isConnectionAlreadyActive ->
						if (isConnectionAlreadyActive) {
							logger.debug("Connection for {} already active.", libraryId)
							manageConnectionSessions.promiseLibraryConnection(libraryId)
						} else {
							logger.debug("Connection for {} not active, creating.", libraryId)
							val promisedConnection = manageConnectionSessions.promiseTestedLibraryConnection(libraryId)
							proxyUpdates(promisedConnection)
							doCancel(promisedConnection)
							promisedConnection
								.inevitably(this@DramaticConnectionInitializationController)
								.also(::doCancel)
						}
					}
					.eventually(
						{ c ->
							if (c == null) {
								removeConnection(libraryId)
								applicationNavigation
									.viewApplicationSettings()
									.also(::doCancel)
									.then({ _ -> null }, { null })
							} else {
								c.toPromise()
							}
						},
						{  e ->
							logger.error("An error occurred getting the connection for library ID ${libraryId.id}.", e)
							applicationNavigation.viewApplicationSettings().then({ _ -> null }, { null })
						}
					)

				proxy(promisedConnection)
			}
		}

	override fun promiseAction(): Promise<*> = PromiseDelay.delay<Any?>(dramaticPause)
}
