package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromiseProxy
import org.joda.time.Duration

private val launchDelay by lazy { Duration.standardSeconds(2).plus(Duration.millis(500)) }
private val logger by lazyLogger<ConnectionInitializationController>()

class ConnectionInitializationController(
    private val manageConnectionSessions: ManageConnectionSessions,
	private val applicationNavigation: NavigateApplication
) : ControlConnectionInitialization {
	companion object {
		private val truePromise by lazy { ProgressingPromise<BuildingConnectionStatus, Boolean>(true) }
	}

	override fun promiseInitializedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, Boolean> =
		if (manageConnectionSessions.isConnectionActive(libraryId)) truePromise
		else object : ProgressingPromiseProxy<BuildingConnectionStatus, Boolean>() {
			init {
			    val promisedConnection = manageConnectionSessions.promiseLibraryConnection(libraryId)
				doCancel(promisedConnection)
				proxyUpdates(promisedConnection)
				promisedConnection.then(
					{
						resolve(it != null)
						if (it == null)	launchSettingsDelayed()
					},
					{  e ->
						logger.error("An error occurred getting the connection for library ID ${libraryId.id}.", e)
						resolve(false)
						launchSettingsDelayed()
					}
				)
			}
		}

	private fun launchSettingsDelayed() {
		PromiseDelay
			.delay<Any?>(launchDelay)
			.then { applicationNavigation.launchSettings() }
	}
}
