package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsBeingInitialized

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
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
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

private const val libraryId = 267

class `when calling the status check again with another library` {
	private val mut by lazy {
		val resolvingConnection = DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()
		Pair(
			resolvingConnection,
			ConnectionStatusViewModel(
				FakeStringResources(
					connected = "x7lcvspHV",
					connecting = "5MmZ6OPl",
					gettingLibrary = "vIQXMhBV5t5",
					connectingToServerLibrary = "2bsRkyrQO",
				),
				mockk {
					every { promiseLibraryConnection(LibraryId(480)) } answers {
						val deferredProgressingPromise = DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()
						deferredProgressingPromise.sendProgressUpdate(BuildingConnectionStatus.BuildingConnection)
						deferredProgressingPromise
					}
					every { promiseLibraryConnection(LibraryId(libraryId)) } returns resolvingConnection
				},
			)
		)
	}

	private val connectionStatuses = mutableListOf<String>()
	private val isConnectingHistory = mutableListOf<Boolean>()

	private var firstPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>? = null
	private var secondPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>? = null

	private var cancellationException: CancellationException? = null

	private var secondLibraryConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		val (resolvingConnection, viewModel) = mut

		viewModel.connectionStatus.mapNotNull().subscribe(connectionStatuses::add).toCloseable().use {
			viewModel.isGettingConnection.mapNotNull().subscribe(isConnectingHistory::add).toCloseable().use {
				firstPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(480))
				secondPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(libraryId))

				try {
					firstPromisedLibraryConnection?.toExpiringFuture()?.get()
				} catch (ee: ExecutionException) {
					cancellationException = ee.cause as? CancellationException
				}

				with (resolvingConnection) {
					sendProgressUpdate(BuildingConnectionStatus.GettingLibrary)
					sendResolution(mockk())
				}

				secondLibraryConnection = secondPromisedLibraryConnection?.toExpiringFuture()?.get()
			}
		}
	}

	@Test
	fun `then the connecting statuses are correct`() {
		assertThat(connectionStatuses).containsExactly("", "5MmZ6OPl", "2bsRkyrQO", "5MmZ6OPl", "vIQXMhBV5t5", "x7lcvspHV")
	}

	@Test
	fun `then is connecting status history is correct`() {
		assertThat(isConnectingHistory).containsExactly(false, true, false, true, false)
	}

	@Test
	fun `then the correct connection is returned`() {
		assertThat(secondLibraryConnection).isNotNull
	}

	@Test
	fun `then the first connection is cancelled`() {
		assertThat(cancellationException).isNotNull
	}

	@Test
	fun `then the second promised connection is NOT the same as the first`() {
		assertThat(secondPromisedLibraryConnection).isNotSameAs(firstPromisedLibraryConnection)
	}
}
