package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive.AndTheConnectionIsRemoved

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenGettingTheUpdatedConnection {

	companion object {
		private var isActive: Boolean? = null
		private var originalConnection: IConnectionProvider? = null
		private var newConnection: IConnectionProvider? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val connectionsTester = mockk<TestConnections>()
			every  { connectionsTester.promiseIsConnectionPossible(any()) } returns true.toPromise()

			val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
			every { libraryConnectionProvider.promiseLibraryConnection(LibraryId(2)) } answers { ProgressingPromise(mockk<IConnectionProvider>()) }

			val connectionSessionManager = ConnectionSessionManager(
				connectionsTester,
				libraryConnectionProvider
			)

			val libraryId = LibraryId(2)
			val futureConnectionProvider = connectionSessionManager.promiseLibraryConnection(libraryId).toFuture()

			originalConnection = futureConnectionProvider[30, TimeUnit.SECONDS]

			connectionSessionManager.removeConnection(libraryId)

			isActive = connectionSessionManager.isConnectionActive(libraryId)

			newConnection = connectionSessionManager.promiseLibraryConnection(libraryId).toFuture()[30, TimeUnit.SECONDS]
		}
	}

	@Test
	fun thenTheConnectionIsNotActive() {
		assertThat(isActive).isFalse
	}

	@Test
	fun thenTheNewConnectionIsNotTheOriginalConnection() {
		assertThat(newConnection).isNotEqualTo(originalConnection)
	}
}
