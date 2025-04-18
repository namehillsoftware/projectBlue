package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndGettingTheConnectionFaults

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
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
            DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		Pair(
			deferredProgressingPromise,
            DramaticConnectionInitializationController(
                mockk {
					every { promiseIsConnectionActive(LibraryId(libraryId)) } returns false.toPromise()
                    every { promiseTestedLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
					every { removeConnection(LibraryId(libraryId)) } answers { removedConnection = firstArg() }
                },
            )
		)
	}
	private val recordedUpdates = mutableListOf<BuildingConnectionStatus>()
	private var initializedConnection: LiveServerConnection? = null
	private var removedConnection: LibraryId? = null

	@BeforeAll
	fun act() {
		val (deferredPromise, controller) = mut
		val promisedConnection = controller
			.promiseLibraryConnection(LibraryId(libraryId))
			.onEach(recordedUpdates::add)

		deferredPromise.sendProgressUpdates(
            BuildingConnectionStatus.BuildingConnection,
            BuildingConnectionStatus.GettingLibrary,
            BuildingConnectionStatus.BuildingConnectionComplete,
		)
		deferredPromise.sendRejection(Exception("boom!"))

		initializedConnection = promisedConnection.toExpiringFuture().get()
	}

	@Test
    fun `then the updates are correct`() {
		assertThat(recordedUpdates).containsExactly(
			BuildingConnectionStatus.BuildingConnection,
			BuildingConnectionStatus.GettingLibrary,
			BuildingConnectionStatus.BuildingConnectionComplete,
		)
	}

	@Test
    fun `then the connection is not initialized`() {
		assertThat(initializedConnection).isNull()
	}

	@Test
	fun `then the connection is removed`() {
		assertThat(removedConnection).isEqualTo(LibraryId(libraryId))
	}
}
