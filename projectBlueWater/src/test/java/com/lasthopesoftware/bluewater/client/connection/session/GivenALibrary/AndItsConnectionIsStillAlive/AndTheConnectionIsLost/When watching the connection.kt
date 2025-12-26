package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive.AndTheConnectionIsLost

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionLostNotification
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionWatcherViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.observables.toCloseable
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 215

class `When watching the connection` {
	private val mut by lazy {
		val messageBus = RecordingApplicationMessageBus()

		Pair(messageBus, ConnectionWatcherViewModel(
			messageBus,
			FakeLibraryConnectionProvider(
				mapOf(
					Pair(LibraryId(libraryId), mockk())
				)
			),
			mockk {
				every { pollConnection(LibraryId(libraryId)) } returns mockk<LiveServerConnection>().toPromise()
			}
		))
	}

	private val checkingConnectionStates = mutableListOf<Boolean>()
	private var isConnectionActive = false

	@BeforeAll
	fun act() {
		val (messageBus, viewModel) = mut
		viewModel.isCheckingConnection.subscribe { checkingConnectionStates.add(it.value) }.toCloseable().use {
			val futureConnection = viewModel.watchLibraryConnection(LibraryId(libraryId)).toExpiringFuture()

			messageBus.sendMessage(ConnectionLostNotification(LibraryId(libraryId)))

			isConnectionActive = futureConnection.get() ?: false
		}
	}

	@Test
	fun `then the checking connection states are correct`() {
		assertThat(checkingConnectionStates).containsExactly(false, true, false)
	}

	@Test
	fun `then the connection is active`() {
		assertThat(isConnectionActive).isTrue()
	}
}
