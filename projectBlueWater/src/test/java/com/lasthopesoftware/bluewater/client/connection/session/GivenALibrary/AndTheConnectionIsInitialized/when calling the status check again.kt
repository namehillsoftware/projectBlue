package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsInitialized

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

private const val libraryId = 107

class `when calling the status check again` {
	private val mut by lazy {
		val firstDeferredProgressingPromise =
            DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()

		val secondDeferredProgressingPromise =
			DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()

		Triple(
			firstDeferredProgressingPromise,
			secondDeferredProgressingPromise,
            ConnectionStatusViewModel(
                FakeStringResources(),
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

	private var firstPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>? = null
	private var secondPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>? = null

	private var firstLibraryConnection: ProvideConnections? = null
	private var secondLibraryConnection: ProvideConnections? = null

	@BeforeAll
	fun act() {
		val (firstDeferredPromise, secondDeferredPromise, viewModel) = mut

		viewModel.isGettingConnection.subscribe { isConnecting -> isConnectingHistory.add(isConnecting.value) }.toCloseable().use {
			firstPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(libraryId))

			firstDeferredPromise.sendResolution(mockk())

			secondPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(libraryId))

			secondDeferredPromise.sendResolution(mockk())

			firstLibraryConnection = firstPromisedLibraryConnection?.toExpiringFuture()?.get()
			secondLibraryConnection = secondPromisedLibraryConnection?.toExpiringFuture()?.get()
		}
	}

	@Test
	fun `then is connecting status history is correct`() {
		assertThat(isConnectingHistory).containsExactly(false, true, false)
	}

	@Test
	fun `then the correct connection is returned`() {
		assertThat(firstLibraryConnection).isNotNull
	}
}
