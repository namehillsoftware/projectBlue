package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive.AndTheConnectionChanges

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.onEach
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

private const val libraryId = 378

class `When initializing the connection twice` {

	private val mut by lazy {
		val deferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		Pair(
			deferredProgressingPromise,
            DramaticConnectionInitializationController(
                mockk {
                    every { promiseIsConnectionActive(LibraryId(libraryId)) } returns true.toPromise()
                    every { promiseLibraryConnection(LibraryId(libraryId)) } returnsMany listOf(
						deferredProgressingPromise,
						ProgressingPromise(mockk<LiveServerConnection>())
					)
                },
            )
		)
	}

	private val recordedUpdates = mutableListOf<BuildingConnectionStatus>()
	private var firstConnection: LiveServerConnection? = null
	private var secondConnection: LiveServerConnection? = null

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

		firstConnection = isInitializedPromise
			.toExpiringFuture()
			.get(1, TimeUnit.SECONDS)!! // Expect an immediate return

		secondConnection = controller
			.promiseLibraryConnection(LibraryId(libraryId))
			.toExpiringFuture()
			.get(1, TimeUnit.SECONDS)!! // Expect an immediate return
	}

	@Test
	fun `then no updates are recorded`() {
		assertThat(recordedUpdates).isEmpty()
	}

	@Test
    fun `then the connection is initialized`() {
		assertThat(firstConnection).isNotNull
	}

	@Test
	fun `then the second connection is not the same as the first`() {
		assertThat(secondConnection).isNotEqualTo(firstConnection)
	}
}
