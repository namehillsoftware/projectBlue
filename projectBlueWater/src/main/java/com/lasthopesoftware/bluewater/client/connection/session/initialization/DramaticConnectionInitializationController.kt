package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse

class DramaticConnectionInitializationController(
    private val inner: ControlConnectionInitialization,
    private val navigateApplication: NavigateApplication,
    private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
) : ControlConnectionInitialization {
	var previouslySelectedLibraryId: LibraryId? = null

	override fun promiseInitializedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>(),
            PromisedResponse<IConnectionProvider?, Unit> {
			init {
				val promisedConnection = inner.promiseInitializedConnection(libraryId)
				proxyRejection(promisedConnection)
				promisedConnection.eventually(this)
			}

			override fun promiseResponse(connection: IConnectionProvider?): Promise<Unit> = selectedLibraryIdProvider
				.promiseSelectedLibraryId()
				.eventually { selectedLibraryId ->
					if (previouslySelectedLibraryId == selectedLibraryId) {
						selectedLibraryId.toPromise()
					} else {
						previouslySelectedLibraryId = selectedLibraryId
						PromiseDelay.delay<Any?>(ConnectionInitializationConstants.dramaticPause).then { selectedLibraryId }
					}
				}
				.eventually {
					it?.let(navigateApplication::viewLibrary)
				}
				.then({ resolve(connection) }, ::reject)
		}
}
