package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsNotAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionInitializationController
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeoutException

private const val libraryId = 148

class `when initializing its connection` {
	private val settingsLaunchedLatch = DeferredPromise(true)

	private val mut by lazy {
		val deferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

		Pair(
			deferredProgressingPromise,
            ConnectionInitializationController(
                mockk {
                    every { isConnectionActive(LibraryId(libraryId)) } returns false

                    every { promiseLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
                },
				mockk {
					every { launchSettings() } answers {
						settingsLaunchedLatch.resolve()
					}
				},
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
		)
		deferredPromise.sendResolution(mockk())

		isInitialized = isInitializedPromise
			.toExpiringFuture()
			.get()!!
	}

	@Test
    fun `then the updates are correct`() {
		assertThat(recordedUpdates).containsExactly(
            BuildingConnectionStatus.BuildingConnection,
            BuildingConnectionStatus.GettingLibrary,
            BuildingConnectionStatus.SendingWakeSignal,
		)
	}

	@Test
    fun `then the connection is initialized`() {
		assertThat(isInitialized).isTrue
	}

	@Test
	fun `then the settings are not launched`() {
		try {
			settingsLaunchedLatch.toExpiringFuture().get()
			Assertions.fail("Settings launched")
		} catch (e: TimeoutException) {
			assertThat(e).isNotNull
		}
	}
}
