package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary.AndGettingALiveUrlFails

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.BuildingSessionConnectionStatus.BuildingConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.BuildingSessionConnectionStatus.BuildingConnectionFailed
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.BuildingSessionConnectionStatus.GettingLibrary
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.resources.FakeMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenRetrievingTheSelectedConnection : AndroidContext() {

	companion object {
		private val fakeMessageSender = lazy { FakeMessageBus(ApplicationProvider.getApplicationContext()) }
		private var connectionProvider: IConnectionProvider? = null
	}

	override fun before() {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
		val libraryConnections = mockk<ManageConnectionSessions>()
		every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

		val libraryIdentifierProvider = mockk<ISelectedLibraryIdentifierProvider>()
		every { libraryIdentifierProvider.selectedLibraryId } returns LibraryId(2)
		SelectedConnectionReservation().use {
			val sessionConnection = SelectedConnection(fakeMessageSender.value, libraryIdentifierProvider, libraryConnections)
			val futureConnectionProvider = FuturePromise(sessionConnection.promiseSessionConnection())
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibrary)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnection)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionFailed)
			deferredConnectionProvider.sendResolution(null)
			connectionProvider = futureConnectionProvider.get()
		}
	}

	@Test
	fun thenAConnectionProviderIsNotReturned() {
		assertThat(connectionProvider).isNull()
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(
			fakeMessageSender.value.recordedIntents
			.map { i -> i.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1) }
			.toList())
			.containsExactly(GettingLibrary, BuildingConnection, BuildingConnectionFailed)
	}
}