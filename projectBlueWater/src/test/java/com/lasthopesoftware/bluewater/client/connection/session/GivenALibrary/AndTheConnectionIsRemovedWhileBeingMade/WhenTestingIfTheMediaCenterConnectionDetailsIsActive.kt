package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsRemovedWhileBeingMade

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenTestingIfTheMediaCenterConnectionDetailsIsActive {

	private val libraryId = LibraryId(85)

	private val mut by lazy {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		val connectionSessionManager = ConnectionSessionManager(
            mockk {
				every { promiseLibraryConnection(libraryId) } returns deferredConnectionProvider
			},
			PromisedConnectionsRepository(),
			RecordingApplicationMessageBus()
		)

		connectionSessionManager
	}

	private var cancellationException: CancellationException? = null
	private var isActive: Boolean? = null

	@BeforeAll
	fun before() {
		val futureConnectionProvider = mut.promiseLibraryConnection(libraryId).toExpiringFuture()

		mut.removeConnection(libraryId)

		try {
			futureConnectionProvider[30, TimeUnit.SECONDS]
		} catch (e: ExecutionException) {
			cancellationException = e.cause as? CancellationException ?: throw e
		}

		isActive = mut.promiseIsConnectionActive(libraryId).toExpiringFuture().get() ?: false
	}

	@Test
	fun `then the connection is not active`() {
		assertThat(isActive).isFalse
	}

	@Test
	fun `then the connection provider is cancelled`() {
		assertThat(cancellationException).isNotNull
	}
}
