package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndTheConnectionIsBeingInitialized

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 176

class `when calling the status check again` {
	private val mut by lazy {
		val deferredProgressingPromise =
			DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		Pair(
			deferredProgressingPromise,
			ConnectionStatusViewModel(
				FakeStringResources(),
				mockk {
					every { promiseLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
				},
				RecordingApplicationMessageBus(),
			)
		)
	}

	private val isConnectingHistory = mutableListOf<Boolean>()

	private var firstPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>? = null
	private var secondPromisedLibraryConnection: ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>? = null

	private var firstLibraryConnection: LiveServerConnection? = null
	private var secondLibraryConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		val (deferredPromise, viewModel) = mut

		viewModel.isGettingConnection.subscribe { isConnecting -> isConnectingHistory.add(isConnecting.value) }.toCloseable().use {
			firstPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(libraryId))
			secondPromisedLibraryConnection = viewModel.promiseLibraryConnection(LibraryId(libraryId))

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
