package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheReturnedConnectionIsNull

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.onEach
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 388

class `when initializing its connection` {
	private val mut by lazy {
		val deferredProgressingPromise =
			DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()

		Pair(
			deferredProgressingPromise,
			DramaticConnectionInitializationController(
                mockk {
                    every { removeConnection(LibraryId(libraryId)) } answers {
                        isConnectionRemoved = true
                    }
                    every { promiseIsConnectionActive(LibraryId(libraryId)) } returns true.toPromise()
                    every { promiseLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
                },
            )
		)
	}

	private var isConnectionRemoved = false
	private val recordedUpdates = mutableListOf<BuildingConnectionStatus>()
	private var initializedConnection: ProvideConnections? = null

	@BeforeAll
	fun act() {
		val (deferredPromise, controller) = mut
		val promisedConnection = controller
			.promiseLibraryConnection(LibraryId(libraryId))
			.onEach(recordedUpdates::add)

		deferredPromise.sendProgressUpdates(
			BuildingConnectionStatus.BuildingConnection,
			BuildingConnectionStatus.GettingLibrary,
			BuildingConnectionStatus.SendingWakeSignal,
			BuildingConnectionStatus.BuildingConnectionComplete,
			BuildingConnectionStatus.BuildingConnectionFailed,
		)
		deferredPromise.sendResolution(null)

		initializedConnection = promisedConnection.toExpiringFuture().get()
	}

	@Test
	fun `then the updates are empty`() {
		assertThat(recordedUpdates).isEmpty()
	}

	@Test
	fun `then the connection is not initialized`() {
		assertThat(initializedConnection).isNull()
	}

	@Test
	fun `then the connection is removed`() {
		assertThat(isConnectionRemoved).isTrue
	}
}
