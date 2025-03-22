package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsInitialized

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 107

class `when calling the status check again` {
	private val mut by lazy {
		val firstDeferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()
		firstDeferredProgressingPromise.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionComplete)

		val secondDeferredProgressingPromise =
			DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()
		secondDeferredProgressingPromise.sendProgressUpdate(BuildingConnectionStatus.BuildingConnectionComplete)

		Triple(
			firstDeferredProgressingPromise,
			secondDeferredProgressingPromise,
			ConnectionStatusViewModel(
				FakeStringResources(
					connected = "x7lcvspHV",
					connecting = "5MmZ6OPl",
					connectingToServerLibrary = "2bsRkyrQO",
				),
				mockk {
					every { promiseLibraryConnection(LibraryId(libraryId)) } returnsMany listOf(
						firstDeferredProgressingPromise,
						secondDeferredProgressingPromise
					)
				},
			)
		)
	}

	private val isConnectingHistory = mutableListOf<Boolean>()
	private val connectionStatusHistory = mutableListOf<String>()

	private var firstLibraryConnection: LiveServerConnection? = null
	private var secondLibraryConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		val (firstDeferredPromise, secondDeferredPromise, viewModel) = mut

		with (viewModel) {
			connectionStatus.subscribe { status -> connectionStatusHistory.add(status.value) }.toCloseable().use {
				isGettingConnection.subscribe { isConnecting -> isConnectingHistory.add(isConnecting.value) }.toCloseable().use {
					val futureFirstConnection = promiseLibraryConnection(LibraryId(libraryId)).toExpiringFuture()

					with (firstDeferredPromise) {
						sendProgressUpdates(
							BuildingConnectionStatus.BuildingConnection,
							BuildingConnectionStatus.BuildingConnectionComplete,
						)

						sendResolution(mockk())
					}

					firstLibraryConnection = futureFirstConnection.get()

					val futureSecondConnection = promiseLibraryConnection(LibraryId(libraryId)).toExpiringFuture()

					with (secondDeferredPromise) {
						sendProgressUpdates(
							BuildingConnectionStatus.BuildingConnection,
							BuildingConnectionStatus.BuildingConnectionComplete,
						)

						sendResolution(mockk())
					}

					secondLibraryConnection = futureSecondConnection.get()
				}
			}
		}
	}

	@Test
	fun `then is connecting history is correct`() {
		assertThat(isConnectingHistory).containsExactly(false, true, false)
	}

	@Test
	fun `then the connection status history does NOT contain initial BuildingConnectionComplete from second attempt because it is assumed the connection is healthy`() {
		assertThat(connectionStatusHistory).containsExactly(
			"",
			"5MmZ6OPl",
			"x7lcvspHV",
			"2bsRkyrQO",
			"x7lcvspHV",
			"5MmZ6OPl",
			"2bsRkyrQO",
			"x7lcvspHV",
		)
	}

	@Test
	fun `then the correct connection is returned`() {
		assertThat(firstLibraryConnection).isNotNull
	}

	@Test
	fun `then the correct connection is returned again`() {
		assertThat(secondLibraryConnection).isNotNull
	}
}
