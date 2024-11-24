package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 948

class `When testing the connection` {

	private val mut by lazy {
		val deferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()

		Pair(
			deferredProgressingPromise,
            DramaticConnectionInitializationController(
                mockk {
                    every { promiseTestedLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
                },
                mockk(),
            )
		)
	}

	private val recordedUpdates = mutableListOf<BuildingConnectionStatus>()
	private var initializedConnection: ProvideConnections? = null
	private var initializationTime = Duration.ZERO

	@BeforeAll
	fun act() {
		val startTime = DateTime.now()
		val (deferredPromise, controller) = mut
		val isInitializedPromise = controller
			.promiseTestedLibraryConnection(LibraryId(libraryId))
			.apply { updates(recordedUpdates::add) }

		deferredPromise.sendProgressUpdates(
			BuildingConnectionStatus.BuildingConnection,
			BuildingConnectionStatus.GettingLibrary,
			BuildingConnectionStatus.SendingWakeSignal,
		)
		deferredPromise.sendResolution(mockk())

		initializedConnection = isInitializedPromise
			.toExpiringFuture()
			.get()

		initializationTime = Duration(startTime, DateTime.now())
	}

	@Test
	fun `then updates are recorded`() {
		assertThat(recordedUpdates).containsExactly(
			BuildingConnectionStatus.BuildingConnection,
			BuildingConnectionStatus.GettingLibrary,
			BuildingConnectionStatus.SendingWakeSignal,
		)
	}

	@Test
    fun `then the connection is initialized`() {
		assertThat(initializedConnection).isNotNull
	}

	@Test
	fun `then return is delayed`() {
		assertThat(initializationTime.millis).isGreaterThanOrEqualTo(2500)
	}
}
