package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

class WhenRetrievingTheTestedSelectedConnectionOnBuildComplete {

	private val applicationMessageBus = RecordingApplicationMessageBus()
	private val urlProvider = mockk<IUrlProvider>()
	private var connectionProvider: IConnectionProvider? = null
	private var secondConnectionProvider: IConnectionProvider? = null

	@BeforeAll
	fun act() {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
		val libraryConnections = mockk<ManageConnectionSessions>()
		every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider
		every { libraryConnections.promiseTestedLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

		val libraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
		every { libraryIdentifierProvider.selectedLibraryId } returns Promise(LibraryId(2))
		SelectedConnectionReservation().use {
			val sessionConnection = SelectedConnection(
				applicationMessageBus,
				libraryIdentifierProvider,
				libraryConnections
			)
			deferredConnectionProvider.sendProgressUpdates(BuildingConnectionStatus.GettingLibrary)

			val futureConnectionProvider = sessionConnection.promiseTestedSessionConnection().toExpiringFuture()
			var futureSecondConnectionProvider: Future<IConnectionProvider?>? = null
			deferredConnectionProvider.updates {
				if (it == BuildingConnectionStatus.BuildingConnectionComplete) {
					futureSecondConnectionProvider = sessionConnection.promiseSessionConnection().toExpiringFuture()
				}
			}
			deferredConnectionProvider.sendProgressUpdates(
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
			deferredConnectionProvider.sendResolution(ConnectionProvider(urlProvider, OkHttpFactory))
			connectionProvider = futureConnectionProvider.get()
			secondConnectionProvider = futureSecondConnectionProvider?.get()
		}
	}

	@Test
	fun `then the connection is correct`() {
		assertThat(connectionProvider?.urlProvider).isEqualTo(urlProvider)
	}

	@Test
	fun `then the second connection is correct`() {
		assertThat(secondConnectionProvider).isEqualTo(connectionProvider)
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
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}
}
