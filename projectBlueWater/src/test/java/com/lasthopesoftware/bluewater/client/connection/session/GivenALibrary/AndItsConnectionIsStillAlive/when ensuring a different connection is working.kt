package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
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

private const val originalLibraryId = 907
private const val libraryId = 220

class `when ensuring a different connection is working` {
	private val mut by lazy {
		val deferredProgressingPromise =
			DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		Pair(
			deferredProgressingPromise,
			ConnectionStatusViewModel(
				FakeStringResources(),
				mockk {
					every { promiseLibraryConnection(LibraryId(originalLibraryId)) } returns ProgressingPromise(mockk<LiveServerConnection>())
					every { promiseLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
				},
				RecordingApplicationMessageBus(),
			)
		)
	}

	private var testedLibraryIdDuringCheck: LibraryId? = null
	private var isConnectingDuringCheck = false
	private var isConnectingBeforeCheck = true
	private var isInitialized = false

	@BeforeAll
	fun act() {
		val (deferredPromise, viewModel) = mut

		viewModel.initializeConnection(LibraryId(originalLibraryId)).toExpiringFuture().get()

		isConnectingBeforeCheck = viewModel.isGettingConnection.value
		val isInitializedPromise = viewModel.initializeConnection(LibraryId(libraryId))
		deferredPromise.sendProgressUpdate(BuildingConnectionStatus.GettingLibrary)
		isConnectingDuringCheck = viewModel.isGettingConnection.value
		deferredPromise.sendResolution(mockk())

		isInitialized = isInitializedPromise
			.toExpiringFuture()
			.get()!!
	}

	@Test
	fun `then the connection is initialized`() {
		assertThat(isInitialized).isTrue
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
		assertThat(mut.second.isGettingConnection.value).isFalse
	}

	@Test
	fun `then the tested library id while checking the connection is correct`() {
		assertThat(testedLibraryIdDuringCheck).isNull()
	}
}
