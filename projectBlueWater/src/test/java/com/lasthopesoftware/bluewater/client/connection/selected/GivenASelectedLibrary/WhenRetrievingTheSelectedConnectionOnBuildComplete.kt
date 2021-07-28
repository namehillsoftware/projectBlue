package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary

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
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.resources.FakeMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.Future

class WhenRetrievingTheSelectedConnectionOnBuildComplete : AndroidContext() {

	companion object {
		private val fakeMessageSender = lazy { FakeMessageBus(ApplicationProvider.getApplicationContext()) }
		private val urlProvider = mockk<IUrlProvider>()
		private var connectionProvider: IConnectionProvider? = null
		private var secondConnectionProvider: IConnectionProvider? = null
	}

	override fun before() {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
		val libraryConnections = mockk<ManageConnectionSessions>()
		every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

		val libraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
		every { libraryIdentifierProvider.selectedLibraryId } returns LibraryId(2)
		SelectedConnectionReservation().use {
			val sessionConnection = SelectedConnection(fakeMessageSender.value, libraryIdentifierProvider, libraryConnections)
			deferredConnectionProvider.sendProgressUpdates(
				BuildingConnectionStatus.GettingLibrary,
			)
			val futureConnectionProvider = sessionConnection.promiseSessionConnection().toFuture()
			var futureSecondConnectionProvider: Future<IConnectionProvider?>? = null
			deferredConnectionProvider.updates {
				if (it == BuildingConnectionStatus.BuildingConnectionComplete) {
					futureSecondConnectionProvider = sessionConnection.promiseSessionConnection().toFuture()
				}
			}
			deferredConnectionProvider.sendProgressUpdates(
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
			deferredConnectionProvider.sendResolution(ConnectionProvider(urlProvider, OkHttpFactory.getInstance()))
			connectionProvider = futureConnectionProvider.get()
			secondConnectionProvider = futureSecondConnectionProvider?.get()
		}
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(connectionProvider?.urlProvider).isEqualTo(urlProvider)
	}

	@Test
	fun thenTheSecondConnectionIsCorrect() {
		assertThat(secondConnectionProvider).isEqualTo(connectionProvider)
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(
			fakeMessageSender.value.recordedIntents
			.map { i -> i.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1) })
			.containsExactly(GettingLibrary, BuildingConnection, BuildingSessionComplete)
	}
}
