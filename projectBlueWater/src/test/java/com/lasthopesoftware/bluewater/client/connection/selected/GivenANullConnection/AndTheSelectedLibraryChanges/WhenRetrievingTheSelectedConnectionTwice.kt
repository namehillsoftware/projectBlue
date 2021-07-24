package com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider
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
import com.lasthopesoftware.resources.FakeMessageSender
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.Test

class WhenRetrievingTheSelectedConnectionTwice : AndroidContext() {

	companion object {
		private val firstUrlProvider = mockk<IUrlProvider>()
		private var connectionProvider: IConnectionProvider? = null
	}

	override fun before() {
		val libraryConnections = mockk<ManageConnectionSessions>()
		every { libraryConnections.promiseLibraryConnection(any()) } returns ProgressingPromise(null as IConnectionProvider?)
		every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns ProgressingPromise(ConnectionProvider(
			firstUrlProvider, OkHttpFactory.getInstance()))

		val fakeSelectedLibraryProvider = FakeSelectedLibraryProvider()
		SelectedConnectionReservation().use {
			fakeSelectedLibraryProvider.selectedLibraryId = LibraryId(-1)
			val selectedConnection = SelectedConnection(
				FakeMessageSender(ApplicationProvider.getApplicationContext()),
				fakeSelectedLibraryProvider,
				libraryConnections
			)
			connectionProvider = FuturePromise(selectedConnection.promiseSessionConnection()).get()
			fakeSelectedLibraryProvider.selectedLibraryId = LibraryId(2)
			connectionProvider = FuturePromise(selectedConnection.promiseSessionConnection()).get()
		}
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		Assertions.assertThat(connectionProvider!!.urlProvider).isEqualTo(firstUrlProvider)
	}

	private class FakeSelectedLibraryProvider : ISelectedLibraryIdentifierProvider {
		override var selectedLibraryId: LibraryId? = LibraryId(0)
	}
}
