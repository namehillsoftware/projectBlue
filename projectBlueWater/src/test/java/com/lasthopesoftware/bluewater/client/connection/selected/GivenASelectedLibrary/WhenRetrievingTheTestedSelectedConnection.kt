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
import org.junit.BeforeClass
import org.junit.Test

class WhenRetrievingTheTestedSelectedConnection {

	companion object {
		private val applicationMessageBus = RecordingApplicationMessageBus()
		private val urlProvider = mockk<IUrlProvider>()
		private var connectionProvider: IConnectionProvider? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
			val libraryConnections = mockk<ManageConnectionSessions>()
			every { libraryConnections.promiseTestedLibraryConnection(LibraryId(51)) } returns deferredConnectionProvider

			val libraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
			every { libraryIdentifierProvider.selectedLibraryId } returns Promise(LibraryId(51))

			SelectedConnectionReservation().use {
				val sessionConnection = SelectedConnection(
					applicationMessageBus,
					libraryIdentifierProvider,
					libraryConnections
				)
				val futureConnectionProvider = sessionConnection.promiseTestedSessionConnection().toExpiringFuture()
				deferredConnectionProvider.sendProgressUpdates(
					BuildingConnectionStatus.GettingLibrary,
					BuildingConnectionStatus.BuildingConnection,
					BuildingConnectionStatus.BuildingConnectionComplete
				)
				deferredConnectionProvider.sendResolution(ConnectionProvider(urlProvider, OkHttpFactory))
				connectionProvider = futureConnectionProvider.get()
			}
		}
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(connectionProvider?.urlProvider).isEqualTo(urlProvider)
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(
			applicationMessageBus.recordedMessages
				.filterIsInstance<SelectedConnection.BuildSessionConnectionBroadcast>()
				.map { it.buildingConnectionStatus })
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete)
	}
}
