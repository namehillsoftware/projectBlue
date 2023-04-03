package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse

class DramaticConnectionInitializationController(
	private val inner: ControlConnectionInitialization,
	private val manageConnectionSessions: ManageConnectionSessions,
) : ControlConnectionInitialization {

	@Synchronized
	override fun promiseInitializedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>(),
            PromisedResponse<IConnectionProvider?, Unit> {

			private val isConnectionAlreadyActive = manageConnectionSessions.isConnectionActive(libraryId)

			init {
				val promisedConnection = inner.promiseInitializedConnection(libraryId)
				proxyRejection(promisedConnection)
				promisedConnection.eventually(this)
			}

			override fun promiseResponse(connection: IConnectionProvider?): Promise<Unit> {
				if (isConnectionAlreadyActive) {
					resolve(connection)
					return Unit.toPromise()
				}

				return PromiseDelay
					.delay<Any?>(ConnectionInitializationConstants.dramaticPause)
					.then({ resolve(connection) }, ::reject)
			}
		}
}
