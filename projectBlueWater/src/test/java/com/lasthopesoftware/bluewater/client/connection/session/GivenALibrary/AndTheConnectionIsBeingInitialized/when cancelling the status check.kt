package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsBeingInitialized

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

private const val libraryId = 182

class `when cancelling the status check` {
	private val mut by lazy {
		val deferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()

		Pair(
			deferredProgressingPromise,
            ConnectionStatusViewModel(
                FakeStringResources(),
                mockk {
                    every { promiseLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
                },
            )
		)
	}
	private var isConnectingDuringCheck = false
	private var isConnectingBeforeCheck = true
	private var isConnectingAfterCheck = true
	private var cancellationException: CancellationException? = null

	@BeforeAll
	fun act() {
		val (deferredPromise, viewModel) = mut

		isConnectingBeforeCheck = viewModel.isGettingConnection.value

		val isInitializedPromise = viewModel.initializeConnection(LibraryId(libraryId))
		isConnectingDuringCheck = viewModel.isGettingConnection.value
		viewModel.cancelCurrentCheck()
		deferredPromise.sendResolution(mockk())

		try {
			isInitializedPromise
				.toExpiringFuture()
				.get()!!
		} catch (e: ExecutionException) {
			cancellationException = e.cause as? CancellationException
		}
		isConnectingAfterCheck = viewModel.isGettingConnection.value
	}

	@Test
	fun `then it is not connecting before the checking the connection`() {
		assertThat(isConnectingBeforeCheck).isFalse
	}

	@Test
	fun `then it is connecting while the checking the connection`() {
		assertThat(isConnectingDuringCheck).isTrue
	}

	@Test
	fun `then it is not connecting after checking the connection`() {
		assertThat(isConnectingAfterCheck).isFalse
	}

	@Test
	fun `then the connection is cancelled`() {
		assertThat(mut.second.isCancelled).isTrue
	}

	@Test
	fun `then the promised connection is cancelled`() {
		assertThat(cancellationException).isNotNull
	}
}
