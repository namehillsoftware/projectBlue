package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary.AndGettingALiveUrlFails

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenRetrievingTheSelectedConnection {

	companion object {
		private val applicationMessageBus = RecordingApplicationMessageBus()
		private var connectionProvider: IConnectionProvider? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
			val libraryConnections = mockk<ManageConnectionSessions>()
			every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

			val libraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
			every { libraryIdentifierProvider.selectedLibraryId } returns Promise(LibraryId(2))
			SelectedConnectionReservation().use {
				val sessionConnection = SelectedConnection(mockk(relaxUnitFun = true), applicationMessageBus, libraryIdentifierProvider, libraryConnections)
				val futureConnectionProvider = FuturePromise(sessionConnection.promiseSessionConnection())
				deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibrary)
				deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnection)
				deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionFailed)
				deferredConnectionProvider.sendResolution(null)
				connectionProvider = futureConnectionProvider.get()
			}
		}
	}

	@Test
	fun thenAConnectionProviderIsNotReturned() {
		assertThat(connectionProvider).isNull()
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
				BuildingConnectionStatus.BuildingConnectionFailed)
	}
}
