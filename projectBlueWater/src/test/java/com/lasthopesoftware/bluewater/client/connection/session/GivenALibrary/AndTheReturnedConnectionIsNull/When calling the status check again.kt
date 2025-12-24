package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheReturnedConnectionIsNull

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.observables.toCloseable
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `when calling the status check again` {
	companion object {
		private const val libraryId = 355
	}

	private val mut by lazy {
		val firstDeferredProgressingPromise =
			DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		val secondDeferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		Triple(
			firstDeferredProgressingPromise,
			secondDeferredProgressingPromise,
            ConnectionStatusViewModel(
                FakeStringResources(
					gettingLibrary = "getting",
					connectingToServerLibrary = "connecting",
					connected = "connected",
					errorConnectingTryAgain = "failed",
				),
                mockk {
                    every { promiseLibraryConnection(LibraryId(libraryId)) } returns
						firstDeferredProgressingPromise andThen
						secondDeferredProgressingPromise
                },
				RecordingApplicationMessageBus(),
            )
		)
	}
	private val isConnectingHistory = mutableListOf<Boolean>()

	private val connectionStatuses = mutableListOf<String>()

	private var firstLibraryConnection: LiveServerConnection? = null

	private var secondLibraryConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		val (firstDeferredPromise, secondDeferredPromise, viewModel) = mut

		viewModel.connectionStatus.subscribe { status -> connectionStatuses.add(status.value) }.toCloseable().use {
			viewModel.isGettingConnection.subscribe { isConnecting -> isConnectingHistory.add(isConnecting.value) }.toCloseable().use {
				val firstPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(
					libraryId
				))

				firstDeferredPromise.sendProgressUpdate(BuildingConnectionStatus.GettingLibrary)
				firstDeferredPromise.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionFailed)
				firstDeferredPromise.sendResolution(null)

				firstLibraryConnection = firstPromisedLibraryConnection.toExpiringFuture().get()

				val secondPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(
					libraryId
				))

				secondDeferredPromise.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionComplete)
				secondDeferredPromise.sendResolution(mockk())

				secondLibraryConnection = secondPromisedLibraryConnection.toExpiringFuture().get()
			}
		}
	}

	@Test
	fun `then is connecting status history is correct`() {
		assertThat(isConnectingHistory).containsExactly(false, true, false, true, false)
	}

	@Test
	fun `then the connecting statuses are correct`() {
		assertThat(connectionStatuses).containsExactly("", "getting", "failed", "", "connected")
	}

	@Test
	fun `then the first connection is correct`() {
		assertThat(firstLibraryConnection).isNull()
	}

	@Test
	fun `then the correct second connection is returned`() {
		assertThat(secondLibraryConnection).isNotNull
	}
}
