package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.LiveServerConnection
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
import java.util.concurrent.TimeUnit

private const val libraryId = 176

class `When initializing the connection` {

	private val mut by lazy {
		val deferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		Pair(
			deferredProgressingPromise,
            DramaticConnectionInitializationController(
                mockk {
                    every { promiseIsConnectionActive(LibraryId(libraryId)) } returns true.toPromise()
                    every { promiseLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
                },
            )
		)
	}

	private val recordedUpdates = mutableListOf<BuildingConnectionStatus>()
	private var initializedConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		val (deferredPromise, controller) = mut
		val isInitializedPromise = controller
			.promiseLibraryConnection(LibraryId(libraryId))
			.onEach(recordedUpdates::add)

		deferredPromise.sendProgressUpdates(
            BuildingConnectionStatus.BuildingConnection,
            BuildingConnectionStatus.GettingLibrary,
            BuildingConnectionStatus.SendingWakeSignal,
		)
		deferredPromise.sendResolution(mockk())

		initializedConnection = isInitializedPromise
			.toExpiringFuture()
			.get(1, TimeUnit.SECONDS)!! // Expect an immediate return
	}

	@Test
	fun `then no updates are recorded`() {
		assertThat(recordedUpdates).isEmpty()
	}

	@Test
    fun `then the connection is initialized`() {
		assertThat(initializedConnection).isNotNull
	}
}
