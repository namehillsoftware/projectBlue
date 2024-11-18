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

class ConnectionStatusViewModel(
	private val stringResources: GetStringResources,
	private val connectionInitializationController: ControlConnectionInitialization
) : ViewModel(), (BuildingConnectionStatus) -> Unit, ImmediateAction, TrackConnectionStatus, ProvideLibraryConnections {

	@Volatile
	private var promisedConnectionCheck = Promise.empty<ProvideConnections?>()

	private val mutableIsGettingConnection = MutableInteractionState(false)
	private val mutableConnectionStatus = MutableInteractionState("")
	private val mutableTestedLibraryId = MutableInteractionState<LibraryId?>(null)

	val connectionStatus = mutableConnectionStatus.asInteractionState()
	val isGettingConnection = mutableIsGettingConnection.asInteractionState()
	val testedLibraryId = mutableTestedLibraryId.asInteractionState()
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

		mutableIsGettingConnection.value = true
		mutableTestedLibraryId.value = null
		mutableConnectionStatus.value = stringResources.connecting

		val promisedConnection = object : ProgressingPromiseProxy<BuildingConnectionStatus, ProvideConnections?>() {
			init {
				val promisedConnection = connectionInitializationController.promiseActiveLibraryConnection(libraryId)
				proxy(promisedConnection)
				promisedConnection.progress.then { p ->
					if (p != null) invoke(p)
					promisedConnection.updates(this@ConnectionStatusViewModel)
				}
				promisedConnection.must(this@ConnectionStatusViewModel)
				promisedConnection
					.then { it ->
						val isConnected = it != null
						if (isConnected) mutableTestedLibraryId.value = libraryId
						mutableConnectionStatus.value =
							if (isConnected) stringResources.connected else stringResources.gettingLibraryFailed
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

	override fun invoke(status: BuildingConnectionStatus) {
		mutableConnectionStatus.value = when (status) {
			BuildingConnectionStatus.GettingLibrary -> stringResources.gettingLibrary
			BuildingConnectionStatus.GettingLibraryFailed -> stringResources.gettingLibraryFailed
			BuildingConnectionStatus.SendingWakeSignal -> stringResources.sendingWakeSignal
			BuildingConnectionStatus.BuildingConnection -> stringResources.connectingToServerLibrary
			BuildingConnectionStatus.BuildingConnectionFailed -> stringResources.errorConnectingTryAgain
			BuildingConnectionStatus.BuildingConnectionComplete -> stringResources.connected
		}
	}

	override fun act() {
		mutableIsGettingConnection.value = false
	}
}
