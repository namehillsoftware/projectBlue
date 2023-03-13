package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromiseProxy

private val logger by lazyLogger<ConnectionInitializationErrorController>()

class ConnectionInitializationErrorController(
	private val inner: ControlConnectionInitialization,
	private val applicationNavigation: NavigateApplication
) : ControlConnectionInitialization {

	override fun promiseInitializedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>() {
			init {
			    val promisedConnection = inner.promiseInitializedConnection(libraryId)
				doCancel(promisedConnection)
				proxyUpdates(promisedConnection)
				promisedConnection.then(
					{ c ->
						if (c == null)	launchSettingsDelayed().also(::doCancel).then { resolve(c) }
						else resolve(c)
					},
					{  e ->
						logger.error("An error occurred getting the connection for library ID ${libraryId.id}.", e)
						launchSettingsDelayed().also(::doCancel).then { resolve(null) }
					}
				)
			}
		}

	private fun launchSettingsDelayed() = CancellableProxyPromise { cp ->
		PromiseDelay
			.delay<Any?>(ConnectionInitializationConstants.dramaticPause)
			.also(cp::doCancel)
			.eventually { applicationNavigation.viewApplicationSettings() }
	}
}
