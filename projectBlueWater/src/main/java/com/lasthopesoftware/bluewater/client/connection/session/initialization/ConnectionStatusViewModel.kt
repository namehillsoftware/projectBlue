package com.lasthopesoftware.bluewater.client.connection.session.initialization

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.BuildingConnection
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.BuildingConnectionComplete
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.BuildingConnectionFailed
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.GettingLibrary
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.GettingLibraryFailed
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.SendingWakeSignal
import com.lasthopesoftware.bluewater.client.connection.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.promises.extensions.onEach
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
	private var promisedConnectionCheck = ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>(null as LiveServerConnection?)

	@Volatile
	private var initializingLibraryId = LibraryId(-1)

	@Volatile
	private var testedLibraryId: LibraryId? = null

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
	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?> {
		isCancelled = false

		if (mutableIsGettingConnection.value && libraryId == initializingLibraryId) {
			return promisedConnectionCheck
		}

		promisedConnectionCheck.cancel()

		initializingLibraryId = libraryId

		mutableIsGettingConnection.value = testedLibraryId != libraryId
		mutableConnectionStatus.value = stringResources.connecting

		val promisedConnection = object : ProgressingPromiseProxy<BuildingConnectionStatus, LiveServerConnection?>(libraryConnectionProvider.promiseLibraryConnection(libraryId)),
			ImmediateResponse<LiveServerConnection?, Unit>,
			ImmediateAction,
			(BuildingConnectionStatus) -> Unit {

			init {
				// ViewModel Side-effects
				onEach(this).then(this).must(this)
			}

			override fun respond(connections: LiveServerConnection?) {
				if (initializingLibraryId != libraryId) return

				testedLibraryId = libraryId
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

				when {
					status != BuildingConnectionComplete -> testedLibraryId = null
					testedLibraryId != null -> return
				}

				mutableConnectionStatus.value = when (status) {
					GettingLibrary -> stringResources.gettingLibrary
					GettingLibraryFailed -> stringResources.gettingLibraryFailed
					SendingWakeSignal -> stringResources.sendingWakeSignal
					BuildingConnection -> stringResources.connectingToServerLibrary
					BuildingConnectionFailed -> stringResources.errorConnectingTryAgain
					BuildingConnectionComplete -> stringResources.connected
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
