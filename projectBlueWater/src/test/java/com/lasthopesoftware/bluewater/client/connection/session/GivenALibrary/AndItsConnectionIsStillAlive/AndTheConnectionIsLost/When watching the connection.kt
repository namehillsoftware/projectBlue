package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive.AndTheConnectionIsLost

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionLostNotification
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionWatcherViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 215

class `When watching the connection` {
	private val mut by lazy {
		val deferredConnectionProvider = DeferredPromise(FakeConnectionProvider() as ProvideConnections)

		val messageBus = RecordingApplicationMessageBus()

		Triple(deferredConnectionProvider, messageBus, ConnectionWatcherViewModel(
			messageBus,
			FakeLibraryConnectionProvider(
				mapOf(
					Pair(LibraryId(libraryId), FakeConnectionProvider())
				)
			),
			mockk {
				every { pollConnection(LibraryId(libraryId)) } returns deferredConnectionProvider
			}
		))
	}

	private val checkingConnectionStates = mutableListOf<Boolean>()

	@BeforeAll
	fun act() {
		val (deferredConnection, messageBus, viewModel) = mut
		viewModel.watchLibraryConnection(LibraryId(libraryId))

		checkingConnectionStates.add(viewModel.isCheckingConnection.value)

		messageBus.sendMessage(ConnectionLostNotification(LibraryId(libraryId)))

		checkingConnectionStates.add(viewModel.isCheckingConnection.value)

		deferredConnection.resolve()

		checkingConnectionStates.add(viewModel.isCheckingConnection.value)
	}

	@Test
	fun `then the checking connection states are correct`() {
		assertThat(checkingConnectionStates).containsExactly(false, true, false)
	}
}
