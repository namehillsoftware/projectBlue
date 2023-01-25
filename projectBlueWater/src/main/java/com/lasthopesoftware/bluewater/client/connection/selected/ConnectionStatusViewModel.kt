package com.lasthopesoftware.bluewater.client.connection.selected

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionStatusViewModel(
	private val stringResources: GetStringResources,
	private val sessionConnections: ManageConnectionSessions,
) : ViewModel(), (BuildingConnectionStatus) -> Unit, ImmediateAction
{
	private var promisedConnectionCheck = Promise.empty<IConnectionProvider?>()

	private val isGettingConnectionFlow = MutableStateFlow(false)
	private val connectionStatusFlow = MutableStateFlow("")

	val connectionStatus = connectionStatusFlow.asStateFlow()
	val isGettingConnection = isGettingConnectionFlow.asStateFlow()

	@Synchronized
	fun ensureConnectionIsWorking(libraryId: LibraryId): Promise<*> {
		promisedConnectionCheck.cancel()

		isGettingConnectionFlow.value = true
		connectionStatusFlow.value = stringResources.connecting

		val promisedConnection = sessionConnections.promiseLibraryConnection(libraryId)
		promisedConnection.updates(this)
		promisedConnection.must(this)

		promisedConnectionCheck = promisedConnection

		return promisedConnection
	}

	fun cancelCurrentCheck() {
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
