package com.lasthopesoftware.bluewater.client.connection.session

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.polling.PollForLibraryConnections
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class ConnectionWatcherViewModel(
	messageBus: RegisterForApplicationMessages,
	private val libraryConnections: ProvideLibraryConnections,
	private val pollLibraryConnections: PollForLibraryConnections,
) : ViewModel(), (ConnectionLostNotification) -> Unit {
	@Volatile
	private var promisedConnection = Promise.empty<ProvideConnections?>()

	private var watchedLibraryId: LibraryId? = null

	private val connectionLostSubscription = messageBus.registerReceiver(this)

	private val mutableIsCheckingConnection = MutableInteractionState(false)

	val isCheckingConnection = mutableIsCheckingConnection.asInteractionState()

	fun watchLibraryConnection(libraryId: LibraryId): Promise<Boolean> {
		watchedLibraryId = libraryId
		mutableIsCheckingConnection.value = true
		return libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually(
				{ if (it == null) maybePollConnection(libraryId) else true.toPromise() },
				{ e ->
					if (ConnectionLostExceptionFilter.isConnectionLostException(e)) maybePollConnection(libraryId)
					else false.toPromise()
				}
			)
			.must { _ ->
				if (libraryId == watchedLibraryId)
					mutableIsCheckingConnection.value = false
			}
	}

	fun cancelLibraryConnectionPolling() {
		promisedConnection.cancel()
	}

	override fun onCleared() {
		connectionLostSubscription.close()
	}

	override fun invoke(message: ConnectionLostNotification) {
		maybePollConnection(message.libraryId)
	}

	private fun maybePollConnection(libraryId: LibraryId): Promise<Boolean> {
		if (libraryId != watchedLibraryId) return false.toPromise()

		mutableIsCheckingConnection.value = true
		val proxiedPromisedConnection = Promise.Proxy { cp ->
			pollLibraryConnections
				.pollConnection(libraryId)
				.also(cp::doCancel)
				.must { _ ->
					if (libraryId == watchedLibraryId)
						mutableIsCheckingConnection.value = false
				}
		}

		promisedConnection = proxiedPromisedConnection

		return proxiedPromisedConnection.then { cp -> cp != null && libraryId == watchedLibraryId }
	}
}
