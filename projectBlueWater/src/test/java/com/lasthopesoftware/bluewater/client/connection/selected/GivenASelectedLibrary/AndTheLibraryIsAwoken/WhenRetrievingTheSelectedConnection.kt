package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary.AndTheLibraryIsAwoken

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
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

	private val urlProvider = mockk<IUrlProvider>()
	private val applicationMessageBus = RecordingApplicationMessageBus()
	private var connectionProvider: ProvideConnections? = null

	@BeforeAll
	fun act() {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()
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
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.SendingWakeSignal)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnection)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionComplete)
			deferredConnectionProvider.sendResolution(ConnectionProvider(urlProvider, OkHttpFactory))
			connectionProvider = futureConnectionProvider.get()
		}
	}

	@Test
	fun `then the connection is correct`() {
		assertThat(connectionProvider!!.urlProvider).isEqualTo(urlProvider)
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(
			applicationMessageBus.recordedMessages
				.filterIsInstance<SelectedConnection.BuildSessionConnectionBroadcast>()
				.map { it.buildingConnectionStatus })
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.SendingWakeSignal,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}
}
