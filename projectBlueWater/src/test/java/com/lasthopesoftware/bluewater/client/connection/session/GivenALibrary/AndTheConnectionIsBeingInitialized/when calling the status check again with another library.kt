package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsBeingInitialized

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
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
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

private const val libraryId = 267

class `when calling the status check again with another library` {
	private val mut by lazy {
		val deferredProgressingPromise = DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()

		ConnectionStatusViewModel(
			FakeStringResources(),
			mockk {
				every { promiseLibraryConnection(LibraryId(480)) } returns deferredProgressingPromise
				every { promiseLibraryConnection(LibraryId(libraryId)) } returns ProgressingPromise(mockk<ProvideConnections>())
			},
		)
	}

	private val isConnectingHistory = mutableListOf<Boolean>()

	private var firstPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>? = null
	private var secondPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>? = null

	private var cancellationException: CancellationException? = null

	private var secondLibraryConnection: ProvideConnections? = null

	@BeforeAll
	fun act() {
		val viewModel = mut

		viewModel.isGettingConnection.subscribe { isConnecting -> isConnectingHistory.add(isConnecting.value) }.toCloseable().use {
			firstPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(480))
			secondPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(libraryId))

			try {
				firstPromisedLibraryConnection?.toExpiringFuture()?.get()
			} catch (ee: ExecutionException) {
				cancellationException = ee.cause as? CancellationException
			}

			secondLibraryConnection = secondPromisedLibraryConnection?.toExpiringFuture()?.get()
		}
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
