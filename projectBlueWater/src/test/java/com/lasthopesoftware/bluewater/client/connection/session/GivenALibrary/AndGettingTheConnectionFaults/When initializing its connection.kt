package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndGettingTheConnectionFaults

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ActivityConnectionInitializationController
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 388

class `when initializing its connection` {
	private val settingsLaunchedLatch = DeferredPromise(true)

	private val mut by lazy {
		val deferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

		Pair(
			deferredProgressingPromise,
            ActivityConnectionInitializationController(
                mockk {
                    every { isConnectionActive(LibraryId(libraryId)) } returns false

                    every { promiseLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
                },
				mockk {
					every { launchSettings() } answers {
						settingsLaunchedLatch.resolve()
						Unit.toPromise()
					}
				}
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
            BuildingConnectionStatus.BuildingConnectionComplete,
		)
		deferredPromise.sendRejection(Exception("boom!"))

		isInitialized = isInitializedPromise
			.toExpiringFuture()
			.get()!!
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
		assertThat(isInitialized).isFalse
	}

	@Test
	fun `then the settings are launched`() {
		assertThat(settingsLaunchedLatch.toExpiringFuture().get()).isTrue
	}
}
