package com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenRetrievingTheSelectedConnectionTwice {

	companion object {
		private val firstUrlProvider = mockk<IUrlProvider>()
		private var connectionProvider: IConnectionProvider? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val libraryConnections = mockk<ManageConnectionSessions>()
			every { libraryConnections.promiseLibraryConnection(any()) } returns ProgressingPromise(null as IConnectionProvider?)
			every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns ProgressingPromise(ConnectionProvider(
				firstUrlProvider, OkHttpFactory
			))

			val fakeSelectedLibraryProvider = FakeSelectedLibraryProvider()
			SelectedConnectionReservation().use {
				fakeSelectedLibraryProvider.selectedLibraryId = Promise(LibraryId(-1))
				val selectedConnection = SelectedConnection(
					mockk(relaxUnitFun = true),
					RecordingApplicationMessageBus(),
					fakeSelectedLibraryProvider,
					libraryConnections
				)
				connectionProvider = FuturePromise(selectedConnection.promiseSessionConnection()).get()
				fakeSelectedLibraryProvider.selectedLibraryId = Promise(LibraryId(2))
				connectionProvider = FuturePromise(selectedConnection.promiseSessionConnection()).get()
			}
		}
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(connectionProvider!!.urlProvider).isEqualTo(firstUrlProvider)
	}

	private class FakeSelectedLibraryProvider : ProvideSelectedLibraryId {
		override var selectedLibraryId: Promise<LibraryId?> = Promise(LibraryId(0))
	}
}
