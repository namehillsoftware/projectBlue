package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndGettingTheLibraryFaults

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenTestingIfTheConnectionIsActive {
	@Test
	fun thenTheConnectionIsNotActive() {
		assertThat(isActive).isFalse
	}

	@Test
	fun thenAnIOExceptionIsReturned() {
		assertThat(exception).isNotNull
	}

	companion object {
		private var exception: IOException? = null
		private var isActive = false

		@BeforeClass
		@JvmStatic
		fun before() {
			val deferredConnectionProvider =
                DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

			val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
			every { libraryConnectionProvider.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

			val connectionSessionManager = ConnectionSessionManager(
                mockk(),
                libraryConnectionProvider
            )

			val futureConnectionProvider =
				connectionSessionManager
					.promiseLibraryConnection(LibraryId(2))
					.toFuture()

			deferredConnectionProvider.sendRejection(IOException("OMG"))
			try {
				futureConnectionProvider[30, TimeUnit.SECONDS]
			} catch (e: ExecutionException) {
				if (e.cause is IOException) {
					exception = e.cause as IOException?
				}
			} catch (e: TimeoutException) {
				if (e.cause is IOException) {
					exception = e.cause as IOException?
				}
			}
			isActive = connectionSessionManager.isConnectionActive(LibraryId(2))
		}
	}
}
