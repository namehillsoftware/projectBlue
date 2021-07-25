package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsRemovedWhileBeingMade

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenTestingIfTheConnectionIsActive {

	companion object {
		private var cancellationException: CancellationException? = null
		private var isActive: Boolean? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val connectionsTester = mockk<TestConnections>()
			every  { connectionsTester.promiseIsConnectionPossible(any()) } returns true.toPromise()

			val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
			val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
			every { libraryConnectionProvider.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

			val connectionSessionManager = ConnectionSessionManager(
				connectionsTester,
				libraryConnectionProvider
			)

			val libraryId = LibraryId(2)
			val futureConnectionProvider = connectionSessionManager.promiseLibraryConnection(libraryId).toFuture()

			connectionSessionManager.removeConnection(libraryId)

			try {
				futureConnectionProvider[30, TimeUnit.SECONDS]
			} catch (e: ExecutionException) {
				cancellationException = e.cause as? CancellationException ?: throw e
			}

			isActive = connectionSessionManager.isConnectionActive(libraryId)
		}
	}

	@Test
	fun thenTheConnectionIsNotActive() {
		assertThat(isActive).isFalse
	}

	@Test
	fun thenTheConnectionProviderIsCancelled() {
		assertThat(cancellationException).isNotNull
	}
}
