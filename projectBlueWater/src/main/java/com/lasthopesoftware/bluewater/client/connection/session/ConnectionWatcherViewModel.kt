package com.lasthopesoftware.bluewater.client.connection.session

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.polling.PollForLibraryConnections
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionWatcherViewModel(
	messageBus: RegisterForApplicationMessages,
	private val libraryConnections: ProvideLibraryConnections,
	private val pollLibraryConnections: PollForLibraryConnections,
) : ViewModel(), (ConnectionLostNotification) -> Unit {
	private var promisedConnection = Promise.empty<ProvideConnections?>()

	private var watchedLibraryId: LibraryId? = null

	private val connectionLostSubscription = messageBus.registerReceiver(this)

	private val mutableIsCheckingConnection = MutableStateFlow(false)

	val isCheckingConnection = mutableIsCheckingConnection.asStateFlow()

	fun watchLibraryConnection(libraryId: LibraryId) {
		watchedLibraryId = libraryId
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.then(
				{ if (it == null) maybePollConnection(libraryId) },
				{ e -> if (ConnectionLostExceptionFilter.isConnectionLostException(e)) maybePollConnection(libraryId) }
			)
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

	private fun maybePollConnection(libraryId: LibraryId) {
		if (libraryId != watchedLibraryId) return

		mutableIsCheckingConnection.value = true
		promisedConnection = Promise.Proxy { cp ->
			pollLibraryConnections
				.pollConnection(libraryId)
				.also(cp::doCancel)
				.must { _ ->
					if (libraryId == watchedLibraryId)
						mutableIsCheckingConnection.value = false
				}
		}
	}
}
