package com.lasthopesoftware.bluewater.client.connection.session.GivenASelectedLibrary

import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.BuildingConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.BuildingSessionComplete
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.GettingLibrary
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.resources.FakeMessageSender
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.Mockito

class WhenRetrievingTheSessionConnection : AndroidContext() {

	companion object {
		private val fakeMessageSender = FakeMessageSender()
		private val urlProvider = Mockito.mock(IUrlProvider::class.java)
		private var connectionProvider: IConnectionProvider? = null
	}

	override fun before() {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
		val libraryConnections = mockk<ProvideLibraryConnections>()
		every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

		val libraryIdentifierProvider = mockk<ISelectedLibraryIdentifierProvider>()
		every { libraryIdentifierProvider.selectedLibraryId } returns LibraryId(2)
		SessionConnectionReservation().use {
			val sessionConnection = SessionConnection(fakeMessageSender, libraryIdentifierProvider, libraryConnections)
			val futureConnectionProvider = FuturePromise(sessionConnection.promiseSessionConnection())
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibrary)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnection)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionComplete)
			deferredConnectionProvider.sendResolution(ConnectionProvider(urlProvider, OkHttpFactory.getInstance()))
			connectionProvider = futureConnectionProvider.get()
		}
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		Assertions.assertThat(connectionProvider!!.urlProvider).isEqualTo(urlProvider)
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		Assertions.assertThat(fakeMessageSender.recordedIntents
			.map { i -> i.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1) }
			.toList())
			.containsExactly(GettingLibrary, BuildingConnection, BuildingSessionComplete)
	}
}
