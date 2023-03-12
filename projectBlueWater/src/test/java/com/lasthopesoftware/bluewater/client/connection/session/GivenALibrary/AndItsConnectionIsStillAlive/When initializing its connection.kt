package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ActivityConnectionInitializationController
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 788

class `when initializing its connection` {
	private val mut by lazy {
		val deferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

		Pair(
			deferredProgressingPromise,
            ActivityConnectionInitializationController(
                mockk {
                    every { isConnectionActive(LibraryId(libraryId)) } returns true

                    every { promiseLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
                },
				mockk()
            )
		)
	}

	private val recordedUpdates = mutableListOf<BuildingConnectionStatus>()
	private var isInitialized = false

	@BeforeAll
	fun act() {
		val (deferredPromise, controller) = mut
		val isInitializedPromise = controller
			.promiseInitializedConnection(LibraryId(libraryId))
			.apply { updates(recordedUpdates::add) }

		deferredPromise.sendProgressUpdates(
            BuildingConnectionStatus.BuildingConnection,
            BuildingConnectionStatus.GettingLibrary,
            BuildingConnectionStatus.SendingWakeSignal,
            BuildingConnectionStatus.BuildingConnectionComplete,
		)
		deferredPromise.sendResolution(mockk())

		isInitialized = isInitializedPromise
			.toExpiringFuture()
			.get()!!
	}

	@Test
    fun `then the updates are correct`() {
		assertThat(recordedUpdates).isEmpty()
	}

	@Test
    fun `then the connection is initialized`() {
		assertThat(isInitialized).isTrue
	}
}
