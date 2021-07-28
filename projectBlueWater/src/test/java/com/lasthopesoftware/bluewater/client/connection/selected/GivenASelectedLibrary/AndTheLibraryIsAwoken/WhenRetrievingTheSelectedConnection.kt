package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary.AndTheLibraryIsAwoken

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.BuildingSessionConnectionStatus.BuildingConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.BuildingSessionConnectionStatus.BuildingSessionComplete
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.BuildingSessionConnectionStatus.GettingLibrary
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.BuildingSessionConnectionStatus.SendingWakeSignal
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.resources.FakeMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito

class WhenRetrievingTheSelectedConnection : AndroidContext() {

	companion object {
		private val urlProvider = Mockito.mock(IUrlProvider::class.java)
		private val fakeMessageSender = lazy { FakeMessageBus(ApplicationProvider.getApplicationContext()) }
		private var connectionProvider: IConnectionProvider? = null
	}

	override fun before() {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
		val libraryConnections = mockk<ManageConnectionSessions>()
		every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

		val libraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
		every { libraryIdentifierProvider.selectedLibraryId } returns LibraryId(2)
		SelectedConnectionReservation().use {
			val sessionConnection = SelectedConnection(
				fakeMessageSender.value,
				libraryIdentifierProvider,
				libraryConnections)
			val futureConnectionProvider = FuturePromise(sessionConnection.promiseSessionConnection())
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibrary)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.SendingWakeSignal)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnection)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionComplete)
			deferredConnectionProvider.sendResolution(ConnectionProvider(urlProvider, OkHttpFactory.getInstance()))
			connectionProvider = futureConnectionProvider.get()
		}
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(connectionProvider!!.urlProvider).isEqualTo(urlProvider)
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(
			fakeMessageSender.value.recordedIntents
			.map { i -> i.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1) }
			.toList())
			.containsExactly(GettingLibrary, SendingWakeSignal, BuildingConnection, BuildingSessionComplete)
	}
}
