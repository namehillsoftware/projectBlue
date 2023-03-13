package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheReturnedConnectionIsNull

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionInitializationErrorController
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 388

class `when initializing its connection` {
	private val mut by lazy {
		val deferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

		Pair(
			deferredProgressingPromise,
            ConnectionInitializationErrorController(
                mockk {
                    every { promiseInitializedConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
                },
				mockk {
					every { viewApplicationSettings() } answers {
						isSettingsLaunched = true
						Unit.toPromise()
					}
				},
            )
		)
	}

	private val recordedUpdates = mutableListOf<BuildingConnectionStatus>()
	private var initializedConnection: IConnectionProvider? = null
	private var isSettingsLaunched = false

	@BeforeAll
	fun act() {
		val (deferredPromise, controller) = mut
		val promisedConnection = controller
			.promiseInitializedConnection(LibraryId(libraryId))
			.apply { updates(recordedUpdates::add) }

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
    fun `then the updates are correct`() {
		assertThat(recordedUpdates).containsExactly(
			BuildingConnectionStatus.BuildingConnection,
			BuildingConnectionStatus.GettingLibrary,
			BuildingConnectionStatus.SendingWakeSignal,
			BuildingConnectionStatus.BuildingConnectionComplete,
			BuildingConnectionStatus.BuildingConnectionFailed,
		)
	}

	@Test
    fun `then the connection is not initialized`() {
		assertThat(initializedConnection).isNull()
	}

	@Test
	fun `then the settings are launched`() {
		assertThat(isSettingsLaunched).isTrue
	}
}
