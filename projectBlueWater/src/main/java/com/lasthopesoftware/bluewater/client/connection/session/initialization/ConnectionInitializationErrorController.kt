package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromiseProxy
import java.util.concurrent.CancellationException

private val logger by lazyLogger<ConnectionInitializationErrorController>()

class ConnectionInitializationErrorController(
	private val inner: ControlConnectionInitialization,
	private val applicationNavigation: NavigateApplication
) : ControlConnectionInitialization {

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>() {
			init {
			    val promisedConnection = inner.promiseLibraryConnection(libraryId)
				doCancel(promisedConnection)
				proxyUpdates(promisedConnection)
				promisedConnection.then(
					{ c ->
						if (c == null) applicationNavigation.viewApplicationSettings().also(::doCancel).must { resolve(null) }
						else resolve(c)
					},
					{  e ->
						logger.error("An error occurred getting the connection for library ID ${libraryId.id}.", e)
						val promisedSettingsLaunch =
							if (e is CancellationException) applicationNavigation.viewApplicationSettings().also(::doCancel)
							else applicationNavigation.viewApplicationSettings().also(::doCancel)

						promisedSettingsLaunch.must { resolve(null) }
					}
				)
			}
		}
}
