package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.session.initialization.LibraryConnectionChangedMessage
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenGettingATestedLibraryConnection {

	private val libraryId = LibraryId(722)

	private val mut by lazy {
		val connectionsTester = mockk<TestConnections>()
		every { connectionsTester.promiseIsConnectionPossible(any()) } returns true.toPromise()

		val firstDeferredConnectionProvider =
			DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
		val secondDeferredConnectionProvider =
			DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

		val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
		every {
			libraryConnectionProvider.promiseLibraryConnection(libraryId)
		} returns firstDeferredConnectionProvider andThen secondDeferredConnectionProvider

		val connectionSessionManager = ConnectionSessionManager(
			connectionsTester,
			libraryConnectionProvider,
			PromisedConnectionsRepository(),
			recordingApplicationMessageBus,
		)

		Triple(firstDeferredConnectionProvider, secondDeferredConnectionProvider, connectionSessionManager)
	}

	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()
	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: IConnectionProvider? = null
	private var secondConnectionProvider: IConnectionProvider? = null

	@BeforeAll
	fun act() {
		val (firstDeferredConnectionProvider, secondDeferredConnectionProvider, connectionSessionManager) = mut

		val futureConnectionProvider =
			connectionSessionManager
				.promiseLibraryConnection(libraryId)
				.apply { updates(statuses::add) }
				.toExpiringFuture()

		firstDeferredConnectionProvider.apply {
			sendProgressUpdates(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)

			sendResolution(FakeConnectionProvider())
		}

		connectionProvider = futureConnectionProvider[30, TimeUnit.SECONDS]

		val secondFutureConnectionProvider =
			connectionSessionManager
				.promiseTestedLibraryConnection(libraryId)
				.apply { updates(statuses::add) }
				.toExpiringFuture()

		secondDeferredConnectionProvider.apply {
			sendProgressUpdates(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)

			sendResolution(FakeConnectionProvider())
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
}
