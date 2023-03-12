package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsBeingInitialized

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionInitializationController
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val libraryId = 552

class `when cancelling the initialization` {
	private val settingsLaunchedLatch = CountDownLatch(1)

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
						settingsLaunchedLatch.countDown()
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

		isInitializedPromise.cancel()

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
	fun `then the connection is not initialized`() {
		assertThat(isInitialized).isFalse
	}

	@Test
	fun `then the settings are launched`() {
		assertThat(settingsLaunchedLatch.await(10, TimeUnit.SECONDS)).isTrue
	}
}
