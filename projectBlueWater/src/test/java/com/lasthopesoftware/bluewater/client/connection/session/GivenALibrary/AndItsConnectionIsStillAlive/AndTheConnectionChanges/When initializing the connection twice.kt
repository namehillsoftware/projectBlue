package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive.AndTheConnectionChanges

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.connection.session.initialization.LibraryConnectionChangedMessage
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
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
            DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

		Pair(
			deferredProgressingPromise,
            DramaticConnectionInitializationController(
                mockk {
                    every { promiseIsConnectionActive(LibraryId(libraryId)) } returns true.toPromise()
                    every { promiseLibraryConnection(LibraryId(libraryId)) } returnsMany listOf(
						deferredProgressingPromise,
						ProgressingPromise(FakeConnectionProvider())
					)
                },
                mockk(),
                recordingApplicationMessageBus,
            )
		)
	}

	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()
	private val recordedUpdates = mutableListOf<BuildingConnectionStatus>()
	private var firstConnection: IConnectionProvider? = null
	private var secondConnection: IConnectionProvider? = null

	@BeforeAll
	fun act() {
		val (deferredPromise, controller) = mut
		val isInitializedPromise = controller
			.promiseActiveLibraryConnection(LibraryId(libraryId))
			.apply { updates(recordedUpdates::add) }

		deferredPromise.sendProgressUpdates(
            BuildingConnectionStatus.BuildingConnection,
            BuildingConnectionStatus.GettingLibrary,
            BuildingConnectionStatus.SendingWakeSignal,
		)
		deferredPromise.sendResolution(FakeConnectionProvider())

		firstConnection = isInitializedPromise
			.toExpiringFuture()
			.get(1, TimeUnit.SECONDS)!! // Expect an immediate return

		secondConnection = controller
			.promiseActiveLibraryConnection(LibraryId(libraryId))
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

	@Test
	fun `then two library connection changed updates are sent`() {
		assertThat(recordingApplicationMessageBus.recordedMessages)
			.containsExactly(
				LibraryConnectionChangedMessage(LibraryId(libraryId)),
				LibraryConnectionChangedMessage(LibraryId(libraryId))
			)
	}
}
