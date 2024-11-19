package com.lasthopesoftware.bluewater.client.connection.session.initialization

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class ConnectionStatusViewModel(
	private val stringResources: GetStringResources,
	private val libraryConnectionProvider: ProvideLibraryConnections
) :
	ViewModel(),
	TrackConnectionStatus,
	ProvideLibraryConnections
{

	@Volatile
	private var promisedConnectionCheck = ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>(null as ProvideConnections?)

	@Volatile
	private var initializingLibraryId = LibraryId(-1)

	private val mutableIsGettingConnection = MutableInteractionState(false)
	private val mutableConnectionStatus = MutableInteractionState("")

	val connectionStatus = mutableConnectionStatus.asInteractionState()
	val isGettingConnection = mutableIsGettingConnection.asInteractionState()
	var isCancelled = false
		private set

	override fun initializeConnection(libraryId: LibraryId): Promise<Boolean> = Promise.Proxy { cp ->
		promiseLibraryConnection(libraryId)
			.also(cp::doCancel)
			.then { c -> c != null }
	}

	@Synchronized
	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> {
		isCancelled = false

		if (mutableIsGettingConnection.value && libraryId == initializingLibraryId) {
			return promisedConnectionCheck
		}

		promisedConnectionCheck.cancel()

		initializingLibraryId = libraryId

		mutableIsGettingConnection.value = true
		mutableConnectionStatus.value = stringResources.connecting

		val promisedConnection = object : ProgressingPromiseProxy<BuildingConnectionStatus, ProvideConnections?>(), ImmediateResponse<ProvideConnections?, Unit>, ImmediateAction, (BuildingConnectionStatus) -> Unit {
			init {
				val promisedConnection = libraryConnectionProvider.promiseLibraryConnection(libraryId)
				proxy(promisedConnection)
				promisedConnection.progress.then { p ->
					if (initializingLibraryId == libraryId) {
						if (p != null) invoke(p)
						promisedConnection.updates(this)
					}
				}
				promisedConnection.must(this)
				promisedConnection.then(this)
			}

			override fun respond(connections: ProvideConnections?) {
				if (initializingLibraryId != libraryId) return

				val isConnected = connections != null
				mutableConnectionStatus.value =
					if (isConnected) stringResources.connected else stringResources.gettingLibraryFailed
			}

			override fun act() {
				if (initializingLibraryId == libraryId)
					mutableIsGettingConnection.value = false
			}

			override fun invoke(status: BuildingConnectionStatus) {
				if (initializingLibraryId != libraryId) return

				mutableConnectionStatus.value = when (status) {
					BuildingConnectionStatus.GettingLibrary -> stringResources.gettingLibrary
					BuildingConnectionStatus.GettingLibraryFailed -> stringResources.gettingLibraryFailed
					BuildingConnectionStatus.SendingWakeSignal -> stringResources.sendingWakeSignal
					BuildingConnectionStatus.BuildingConnection -> stringResources.connectingToServerLibrary
					BuildingConnectionStatus.BuildingConnectionFailed -> stringResources.errorConnectingTryAgain
					BuildingConnectionStatus.BuildingConnectionComplete -> stringResources.connected
				}
			}
		}

		promisedConnectionCheck = promisedConnection
		return promisedConnection
	}

	fun cancelCurrentCheck() {
		isCancelled = true
		promisedConnectionCheck.cancel()
	}
}
