package com.lasthopesoftware.bluewater.client.connection.session

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.joda.time.Duration

private val launchDelay by lazy { Duration.standardSeconds(2).plus(Duration.millis(500)) }

class ConnectionStatusViewModel(
	private val stringResources: GetStringResources,
	private val sessionConnections: ManageConnectionSessions,
	private val applicationNavigation: NavigateApplication,
) : ViewModel(), (BuildingConnectionStatus) -> Unit, ImmediateAction, ImmediateResponse<Throwable, Unit>
{
	private var promisedConnectionCheck = Promise.empty<IConnectionProvider?>()

	private val isGettingConnectionFlow = MutableStateFlow(false)
	private val connectionStatusFlow = MutableStateFlow("")

	val connectionStatus = connectionStatusFlow.asStateFlow()
	val isGettingConnection = isGettingConnectionFlow.asStateFlow()
	var isCancelled = false
		private set

	fun ensureConnectionIsWorking(libraryId: LibraryId): Promise<IConnectionProvider?> {
		promisedConnectionCheck.cancel()
		isCancelled = false

		isGettingConnectionFlow.value = true
		connectionStatusFlow.value = stringResources.connecting

		val promisedConnection = sessionConnections.promiseLibraryConnection(libraryId)
		promisedConnection.updates(this)
		promisedConnection.must(this)
		promisedConnection.then(
			{ c ->
				if (c == null) launchSettingsDelayed()
			},
			this)

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

	override fun respond(resolution: Throwable?) {
		launchSettingsDelayed()
	}

	private fun launchSettingsDelayed() {
		PromiseDelay
			.delay<Any?>(launchDelay)
			.then { applicationNavigation.launchSettings() }
	}
}
