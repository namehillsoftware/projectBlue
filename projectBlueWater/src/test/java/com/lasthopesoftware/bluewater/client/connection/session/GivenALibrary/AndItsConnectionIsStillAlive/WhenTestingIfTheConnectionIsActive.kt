package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenTestingIfTheConnectionIsActive {

	private val libraryId = LibraryId(824)

	private val mut by lazy {
		val connectionsTester = mockk<TestConnections>()
		every  { connectionsTester.promiseIsConnectionPossible(any()) } returns true.toPromise()

		val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
		every { libraryConnectionProvider.promiseLibraryConnection(libraryId) } returns ProgressingPromise(mockk<IConnectionProvider>())

		val connectionSessionManager = ConnectionSessionManager(
			connectionsTester,
			libraryConnectionProvider,
			PromisedConnectionsRepository()
		)

		connectionSessionManager
	}

	private var isActive: Boolean? = null

	@BeforeAll
	fun act() {
		mut.promiseLibraryConnection(libraryId).toExpiringFuture()[30, TimeUnit.SECONDS]
		isActive = mut.promiseIsConnectionActive(libraryId).toExpiringFuture().get() ?: false
	}

	@Test
	fun thenTheConnectionIsActive() {
		assertThat(isActive).isTrue
	}
}
