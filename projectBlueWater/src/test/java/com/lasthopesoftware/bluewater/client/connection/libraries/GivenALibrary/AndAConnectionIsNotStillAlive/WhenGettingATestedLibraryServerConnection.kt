package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndAConnectionIsNotStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.onEach
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenGettingATestedLibraryServerConnection {

	private val firstDeferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()
	private val secondDeferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

	private val mut by lazy {
		val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
		every {
			libraryConnectionProvider.promiseLibraryConnection(LibraryId(2))
		} returns firstDeferredConnectionProvider andThen secondDeferredConnectionProvider

		val connectionSessionManager = ConnectionSessionManager(
            libraryConnectionProvider,
			PromisedConnectionsRepository(),
			RecordingApplicationMessageBus())
		connectionSessionManager
	}

	private val statuses = ArrayList<BuildingConnectionStatus>()
	private val expectedConnectionProvider = mockk<LiveServerConnection> {
		every { promiseIsConnectionPossible() } returns false.toPromise()
	}
	private var connectionProvider: LiveServerConnection? = null
	private var secondConnectionProvider: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		val libraryId = LibraryId(2)
		val futureConnectionProvider =
			mut
				.promiseLibraryConnection(libraryId)
				.onEach(statuses::add)
				.toExpiringFuture()

		firstDeferredConnectionProvider.apply {
			sendProgressUpdates(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)

			sendResolution(mockk {
				every { promiseIsConnectionPossible() } returns false.toPromise()
			})
		}

		val secondFutureConnectionProvider =
			mut
				.promiseTestedLibraryConnection(libraryId)
				.onEach(statuses::add)
				.toExpiringFuture()

		secondDeferredConnectionProvider.apply {
			sendProgressUpdates(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)

			sendResolution(expectedConnectionProvider)
		}

		connectionProvider = futureConnectionProvider.get()
		secondConnectionProvider = secondFutureConnectionProvider.get()
	}

	@Test
	fun `then the connection is correct`() {
		assertThat(secondConnectionProvider).isEqualTo(expectedConnectionProvider)
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete,
				BuildingConnectionStatus.BuildingConnectionComplete,
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}
}
