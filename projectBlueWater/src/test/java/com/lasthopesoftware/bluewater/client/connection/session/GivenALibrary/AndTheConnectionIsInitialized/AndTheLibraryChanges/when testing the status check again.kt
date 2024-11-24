package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsInitialized.AndTheLibraryChanges

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.observables.toCloseable
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `when testing the status check again` {
	companion object {
		private const val firstLibraryId = 622
		private const val secondLibraryId = 565
	}

	private val mut by lazy {
		val firstDeferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()

		val secondDeferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()

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
                    every { promiseTestedLibraryConnection(LibraryId(firstLibraryId)) } returns firstDeferredProgressingPromise
                    every { promiseTestedLibraryConnection(LibraryId(secondLibraryId)) } returns secondDeferredProgressingPromise
                },
            )
		)
	}

	private val isConnectingHistory = mutableListOf<Boolean>()
	private val connectionStatuses = mutableListOf<String>()

	private var firstPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>? = null
	private var secondPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>? = null

	private var firstLibraryConnection: ProvideConnections? = null
	private var secondLibraryConnection: ProvideConnections? = null

	@BeforeAll
	fun act() {
		val (firstDeferredPromise, secondDeferredPromise, viewModel) = mut

		viewModel.connectionStatus.subscribe { status -> connectionStatuses.add(status.value) }.toCloseable().use {
			viewModel.isGettingConnection.subscribe { isConnecting -> isConnectingHistory.add(isConnecting.value) }.toCloseable().use {
				firstPromisedLibraryConnection = viewModel.promiseTestedLibraryConnection(
                    LibraryId(
                        firstLibraryId
                    )
                )

				firstDeferredPromise.sendResolution(mockk())

				secondPromisedLibraryConnection = viewModel.promiseTestedLibraryConnection(
                    LibraryId(
                        secondLibraryId
                    )
                )

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
		assertThat(connectionStatuses)
            .containsExactly("", "3DJCi8HY8", "", "9M8CP4o3", "", "3DJCi8HY8")
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
