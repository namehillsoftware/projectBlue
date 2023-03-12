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
private val logger by lazyLogger<ActivityConnectionInitializationController>()

class ActivityConnectionInitializationController(
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
						if (it == null)	launchSettingsDelayed().then { resolve(false) }
						else resolve(true)
					},
					{  e ->
						logger.error("An error occurred getting the connection for library ID ${libraryId.id}.", e)
						launchSettingsDelayed().then { resolve(false) }
					}
				)
			}
		}

	private fun launchSettingsDelayed() =
		PromiseDelay
			.delay<Any?>(launchDelay)
			.eventually { applicationNavigation.viewApplicationSettings() }
}
