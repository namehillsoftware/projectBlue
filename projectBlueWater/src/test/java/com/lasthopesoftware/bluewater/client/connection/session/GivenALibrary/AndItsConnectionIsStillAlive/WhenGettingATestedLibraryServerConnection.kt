package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.LibraryConnectionChangedMessage
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
import java.util.concurrent.TimeUnit

class WhenGettingATestedLibraryServerConnection {

	private val libraryId = LibraryId(722)

	private val mut by lazy {
		val firstDeferredConnectionProvider =
			DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()
		val secondDeferredConnectionProvider =
			DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
		every {
			libraryConnectionProvider.promiseLibraryConnection(libraryId)
		} returns firstDeferredConnectionProvider andThen secondDeferredConnectionProvider

		val connectionSessionManager = ConnectionSessionManager(
			libraryConnectionProvider,
			PromisedConnectionsRepository(),
			recordingApplicationMessageBus,
		)

		Triple(firstDeferredConnectionProvider, secondDeferredConnectionProvider, connectionSessionManager)
	}

	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()
	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: LiveServerConnection? = null
	private var secondConnectionProvider: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		val (firstDeferredConnectionProvider, secondDeferredConnectionProvider, connectionSessionManager) = mut

		val futureConnectionProvider =
			connectionSessionManager
				.promiseLibraryConnection(libraryId)
				.onEach(statuses::add)
				.toExpiringFuture()

		firstDeferredConnectionProvider.apply {
			sendProgressUpdates(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)

			sendResolution(liveConnection())
		}

		connectionProvider = futureConnectionProvider[30, TimeUnit.SECONDS]

		val secondFutureConnectionProvider =
			connectionSessionManager
				.promiseTestedLibraryConnection(libraryId)
				.onEach(statuses::add)
				.toExpiringFuture()

		secondDeferredConnectionProvider.apply {
			sendProgressUpdates(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)

			sendResolution(liveConnection())
		}

		secondConnectionProvider = secondFutureConnectionProvider.get()
	}

	@Test
	fun `then the connection is correct`() {
		assertThat(secondConnectionProvider).isEqualTo(connectionProvider!!)
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}

	@Test
	fun `then a connection changed notification is sent`() {
		assertThat(recordingApplicationMessageBus.recordedMessages).containsExactly(
			LibraryConnectionChangedMessage(libraryId)
		)
	}

	private fun liveConnection() = mockk<LiveServerConnection> {
		every { promiseIsConnectionPossible() } returns true.toPromise()
	}
}
