package com.lasthopesoftware.bluewater.client.connection.session

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionStatusViewModel(
	private val stringResources: GetStringResources,
	private val connectionInitializationController: ControlConnectionInitialization
) : ViewModel(), (BuildingConnectionStatus) -> Unit, ImmediateAction
{
	private var promisedConnectionCheck = false.toPromise()

	private val isGettingConnectionFlow = MutableStateFlow(false)
	private val connectionStatusFlow = MutableStateFlow("")

	val connectionStatus = connectionStatusFlow.asStateFlow()
	val isGettingConnection = isGettingConnectionFlow.asStateFlow()
	var isCancelled = false
		private set

	fun ensureConnectionIsWorking(libraryId: LibraryId): Promise<Boolean> {
		promisedConnectionCheck.cancel()
		isCancelled = false

		isGettingConnectionFlow.value = true
		connectionStatusFlow.value = stringResources.connecting

		val promisedConnection = connectionInitializationController.promiseInitializedConnection(libraryId)
		promisedConnection.updates(this)
		promisedConnection.must(this)
		promisedConnectionCheck = promisedConnection

		return promisedConnection
	}

	fun cancelCurrentCheck() {
		isCancelled = true
		promisedConnectionCheck.cancel()
	}

	override fun invoke(status: BuildingConnectionStatus) {
		connectionStatusFlow.value = when (status) {
			BuildingConnectionStatus.GettingLibrary -> stringResources.gettingLibrary
			BuildingConnectionStatus.GettingLibraryFailed -> stringResources.gettingLibraryFailed
			BuildingConnectionStatus.SendingWakeSignal -> stringResources.sendingWakeSignal
			BuildingConnectionStatus.BuildingConnection -> stringResources.connectingToServerLibrary
			BuildingConnectionStatus.BuildingConnectionFailed -> stringResources.errorConnectingTryAgain
			BuildingConnectionStatus.BuildingConnectionComplete -> stringResources.connected
		}
	}

	override fun act() {
		isGettingConnectionFlow.value = false
	}
}
