package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive.AndTheConnectionIsRemoved

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenGettingTheUpdatedConnection {

	private val libraryId = LibraryId(2)

	private val mut by lazy {
		val connectionsTester = mockk<TestConnections>()
		every { connectionsTester.promiseIsConnectionPossible(any()) } returns true.toPromise()

		val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
		every { libraryConnectionProvider.promiseLibraryConnection(libraryId) } answers { ProgressingPromise(mockk<ProvideConnections>()) }

		val connectionSessionManager = ConnectionSessionManager(
			connectionsTester,
			libraryConnectionProvider,
			PromisedConnectionsRepository(),
			RecordingApplicationMessageBus()
		)

		connectionSessionManager
	}

	private var isActive: Boolean? = null
	private var originalConnection: ProvideConnections? = null
	private var newConnection: ProvideConnections? = null

	@BeforeAll
	fun act() {
		val futureConnectionProvider = mut.promiseLibraryConnection(libraryId).toExpiringFuture()

		originalConnection = futureConnectionProvider[30, TimeUnit.SECONDS]

		mut.removeConnection(libraryId)

		isActive = mut.promiseIsConnectionActive(libraryId).toExpiringFuture().get() ?: false

		newConnection =
			mut.promiseLibraryConnection(libraryId).toExpiringFuture()[30, TimeUnit.SECONDS]
	}

	@Test
	fun `then the connection is not active`() {
		assertThat(isActive).isFalse
	}

	@Test
	fun `then the new connection is not the original connection`() {
		assertThat(newConnection).isNotEqualTo(originalConnection)
	}
}
