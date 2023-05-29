package com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRetrievingTheSelectedConnectionTwice {

	private val firstUrlProvider = mockk<IUrlProvider>()
	private var connectionProvider: IConnectionProvider? = null

	@BeforeAll
	fun act() {
		val libraryConnections = mockk<ManageConnectionSessions>()
		every { libraryConnections.promiseLibraryConnection(any()) } returns ProgressingPromise(null as IConnectionProvider?)
		every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns ProgressingPromise(
			ConnectionProvider(
				firstUrlProvider, OkHttpFactory
			)
		)

		val fakeSelectedLibraryProvider = FakeSelectedLibraryProvider()
		SelectedConnectionReservation().use {
			fakeSelectedLibraryProvider.libraryId = LibraryId(-1)
			val selectedConnection = SelectedConnection(
				RecordingApplicationMessageBus(),
				fakeSelectedLibraryProvider,
				libraryConnections
			)
			connectionProvider = selectedConnection.promiseSessionConnection().toExpiringFuture().get()
			fakeSelectedLibraryProvider.libraryId = LibraryId(2)
			connectionProvider = selectedConnection.promiseSessionConnection().toExpiringFuture().get()
		}
	}

	@Test
	fun `then the connection is correct`() {
		assertThat(connectionProvider!!.urlProvider).isEqualTo(firstUrlProvider)
	}
}
