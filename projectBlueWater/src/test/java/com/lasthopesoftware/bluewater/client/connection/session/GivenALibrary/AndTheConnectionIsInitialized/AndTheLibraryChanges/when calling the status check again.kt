package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsInitialized.AndTheLibraryChanges

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val firstLibraryId = 355
private const val secondLibraryId = 423

class `when calling the status check again` {
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
					gettingLibrary = "VgssfPlPnn1",
					connectingToServerLibrary = "9M8CP4o3",
					connected = "3DJCi8HY8",
				),
                mockk {
                    every { promiseLibraryConnection(LibraryId(firstLibraryId)) } returns firstDeferredProgressingPromise
                    every { promiseLibraryConnection(LibraryId(secondLibraryId)) } returns secondDeferredProgressingPromise
                },
            )
		)
	}

	private val isConnectingHistory = mutableListOf<Boolean>()
	private val connectionStatuses = mutableListOf<String>()

	private var firstPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>? = null
	private var secondPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>? = null

	private var firstLibraryConnection: LiveServerConnection? = null
	private var secondLibraryConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		val (firstDeferredPromise, secondDeferredPromise, viewModel) = mut

		viewModel.connectionStatus.subscribe { status -> connectionStatuses.add(status.value) }.toCloseable().use {
			viewModel.isGettingConnection.subscribe { isConnecting -> isConnectingHistory.add(isConnecting.value) }.toCloseable().use {
				firstPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(firstLibraryId))

				firstDeferredPromise.sendResolution(mockk())

				secondPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(secondLibraryId))

				secondDeferredPromise.sendProgressUpdate(BuildingConnectionStatus.BuildingConnection)
				secondDeferredPromise.sendProgressUpdate(BuildingConnectionStatus.SendingWakeSignal)
				secondDeferredPromise.sendResolution(mockk())

				firstLibraryConnection = firstPromisedLibraryConnection?.toExpiringFuture()?.get()
				secondLibraryConnection = secondPromisedLibraryConnection?.toExpiringFuture()?.get()
			}
		}
	}

	@Test
	fun `then is connecting status history is correct`() {
		assertThat(isConnectingHistory).containsExactly(false, true, false, true, false)
	}

	@Test
	fun `then the connecting statuses are correct`() {
		assertThat(connectionStatuses).containsExactly("", "3DJCi8HY8", "", "9M8CP4o3", "", "3DJCi8HY8")
	}

	@Test
	fun `then the correct connection is returned`() {
		assertThat(firstLibraryConnection).isNotNull
	}

	@Test
	fun `then the correct second connection is returned`() {
		assertThat(secondLibraryConnection).isNotNull
	}
}
