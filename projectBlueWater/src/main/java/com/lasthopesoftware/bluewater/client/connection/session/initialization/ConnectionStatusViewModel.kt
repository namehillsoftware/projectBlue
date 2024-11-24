package com.lasthopesoftware.bluewater.client.connection.session.initialization

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus.BuildingConnectionComplete
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.getStatusString
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class ConnectionStatusViewModel(
	private val stringResources: GetStringResources,
	private val inner: ManageConnectionSessions
) :
	ViewModel(),
	TrackConnectionStatus,
	ManageConnectionSessions by inner
{

	@Volatile
	private var promisedConnectionCheck = ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>(null as ProvideConnections?)

	@Volatile
	private var initializingLibraryId = LibraryId(-1)

	@Volatile
	private var testedLibraryId: LibraryId? = null

	private val connectionSync = Any()

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

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> = synchronized(connectionSync) {
		isCancelled = false

		if (mutableIsGettingConnection.value && libraryId == initializingLibraryId) {
			return promisedConnectionCheck
		}

		promisedConnectionCheck.cancel()

		initializingLibraryId = libraryId

		mutableIsGettingConnection.value = testedLibraryId != libraryId
		mutableConnectionStatus.value = stringResources.connecting

		promiseDecoratedConnection(libraryId, inner::promiseLibraryConnection)
	}

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> = synchronized(connectionSync) {
		isCancelled = false

		if (mutableIsGettingConnection.value && libraryId == initializingLibraryId) {
			return promisedConnectionCheck
		}

		promisedConnectionCheck.cancel()

		initializingLibraryId = libraryId

		mutableIsGettingConnection.value = true
		mutableConnectionStatus.value = stringResources.connecting

		promiseDecoratedConnection(libraryId, inner::promiseTestedLibraryConnection)
	}

	fun cancelCurrentCheck() {
		isCancelled = true
		promisedConnectionCheck.cancel()
	}

	private fun promiseDecoratedConnection(libraryId: LibraryId, connectionProviderCall: (LibraryId) -> ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> = synchronized(connectionSync) {
		val promisedConnection = object : ProgressingPromiseProxy<BuildingConnectionStatus, ProvideConnections?>(), ImmediateResponse<ProvideConnections?, Unit>, ImmediateAction, (BuildingConnectionStatus) -> Unit {
			init {
				val promisedConnection = connectionProviderCall(libraryId)
				proxy(promisedConnection)
				promisedConnection.progress.then { p ->
					if (initializingLibraryId == libraryId) {
						if (p != null && (testedLibraryId != libraryId || p != BuildingConnectionComplete)) invoke(p)
						promisedConnection.updates(this)
					}
				}
				promisedConnection.must(this)
				promisedConnection.then(this)
			}

			override fun respond(connections: ProvideConnections?) {
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

				if (status != BuildingConnectionComplete) {
					testedLibraryId = null
				}

				mutableConnectionStatus.value = stringResources.getStatusString(status)
			}
		}

		promisedConnectionCheck = promisedConnection
		return promisedConnection
	}
}
