package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary.AndGettingALiveUrlFails

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRetrievingTheSelectedConnection {

	private val applicationMessageBus = RecordingApplicationMessageBus()
	private var connectionProvider: IConnectionProvider? = null

	@BeforeAll
	fun act() {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
		val libraryConnections = mockk<ManageConnectionSessions>()
		every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

		val libraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
		every { libraryIdentifierProvider.promiseSelectedLibraryId() } returns Promise(LibraryId(2))
		SelectedConnectionReservation().use {
			val sessionConnection = SelectedConnection(
				applicationMessageBus,
				libraryIdentifierProvider,
				libraryConnections
			)
			val futureConnectionProvider = ExpiringFuturePromise(sessionConnection.promiseSessionConnection())
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibrary)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnection)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionFailed)
			deferredConnectionProvider.sendResolution(null)
			connectionProvider = futureConnectionProvider.get()
		}
	}

	@Test
	fun `then a connection provider is not returned`() {
		assertThat(connectionProvider).isNull()
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(
			applicationMessageBus.recordedMessages
				.filterIsInstance<SelectedConnection.BuildSessionConnectionBroadcast>()
				.map { it.buildingConnectionStatus })
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionFailed
			)
	}
}
