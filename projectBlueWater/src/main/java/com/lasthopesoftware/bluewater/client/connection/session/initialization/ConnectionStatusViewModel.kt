package com.lasthopesoftware.bluewater.client.connection.session.initialization

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionStatusViewModel(
	private val stringResources: GetStringResources,
	private val connectionInitializationController: ControlConnectionInitialization
) : ViewModel(), (BuildingConnectionStatus) -> Unit, ImmediateAction, TrackConnectionStatus {
	private var promisedConnectionCheck = false.toPromise()

	private val mutableIsGettingConnection = MutableStateFlow(false)
	private val mutableConnectionStatus = MutableStateFlow("")
	private val mutableTestedLibraryId = MutableStateFlow<LibraryId?>(null)

	val connectionStatus = mutableConnectionStatus.asStateFlow()
	val isGettingConnection = mutableIsGettingConnection.asStateFlow()
	val testedLibraryId = mutableTestedLibraryId.asStateFlow()
	var isCancelled = false
		private set

	override fun initializeConnection(libraryId: LibraryId): Promise<Boolean> {
		promisedConnectionCheck.cancel()
		isCancelled = false

		mutableIsGettingConnection.value = true
		mutableTestedLibraryId.value = null
		mutableConnectionStatus.value = stringResources.connecting

		val promiseIsConnected = CancellableProxyPromise { cp ->
			val promisedConnection = connectionInitializationController.promiseLibraryConnection(libraryId)
			promisedConnection.progress.then { p ->
				if (p != null) invoke(p)
				promisedConnection.updates(this)
			}
			promisedConnection.must(this)
			promisedConnection
				.also(cp::doCancel)
				.then {
					val isConnected = it != null
					if (isConnected) mutableTestedLibraryId.value = libraryId
					mutableConnectionStatus.value = if (isConnected) stringResources.connected else stringResources.gettingLibraryFailed
					isConnected
				}
		}
		promisedConnectionCheck = promiseIsConnected

		return promiseIsConnected
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
