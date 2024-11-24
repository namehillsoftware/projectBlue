package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsBeingInitialized

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
		private const val libraryId = 265
	}

	private val mut by lazy {
		val deferredProgressingPromise =
			DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()

		Pair(
			deferredProgressingPromise,
			ConnectionStatusViewModel(
				FakeStringResources(),
				mockk {
					every { promiseTestedLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
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
		val (deferredPromise, viewModel) = mut

		viewModel.isGettingConnection.subscribe { isConnecting -> isConnectingHistory.add(isConnecting.value) }.toCloseable().use {
			firstPromisedLibraryConnection = viewModel.promiseTestedLibraryConnection(LibraryId(libraryId))
			secondPromisedLibraryConnection = viewModel.promiseTestedLibraryConnection(LibraryId(libraryId))

			deferredPromise.sendResolution(mockk())

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

	@Test
	fun `then the second promised connection is the same as the first`() {
		assertThat(secondPromisedLibraryConnection).isSameAs(firstPromisedLibraryConnection)
	}
}
