package com.lasthopesoftware.bluewater.client.connection.session

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionLostNotification
import com.lasthopesoftware.bluewater.client.connection.polling.PollForConnections
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionLostViewModel(
	messageBus: RegisterForApplicationMessages,
	private val pollConnections: PollForConnections,
) : ViewModel(), (ConnectionLostNotification) -> Unit {
	private var promisedConnection = Promise.empty<IConnectionProvider?>()

	private var watchedLibraryId: LibraryId? = null

	private val connectionLostSubscription = messageBus.registerReceiver(this)

	private val mutableIsCheckingConnection = MutableStateFlow(false)

	val isCheckingConnection = mutableIsCheckingConnection.asStateFlow()

	fun watchLibraryConnection(libraryId: LibraryId) {
		watchedLibraryId = libraryId
	}

	fun cancelLibraryConnectionPolling() {
		promisedConnection.cancel()
	}

	override fun onCleared() {
		connectionLostSubscription.close()
	}

	override fun invoke(message: ConnectionLostNotification) {
		if (message.libraryId != watchedLibraryId) return

		mutableIsCheckingConnection.value = true
		promisedConnection = pollConnections.pollConnection(message.libraryId).apply {
			must { mutableIsCheckingConnection.value = false }
		}
	}
}
