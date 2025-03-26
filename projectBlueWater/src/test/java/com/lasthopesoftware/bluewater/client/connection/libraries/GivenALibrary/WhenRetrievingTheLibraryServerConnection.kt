package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideProgressingLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRetrievingTheLibraryServerConnection {

	private val expectedConnectionProvider = mockk<LiveServerConnection>()

	private val mut by lazy {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		val libraryConnectionProvider = mockk<ProvideProgressingLibraryConnections>()
		every { libraryConnectionProvider.promiseLibraryConnection(LibraryId(3)) } returns deferredConnectionProvider

		val connectionSessionManager = ConnectionSessionManager(
            libraryConnectionProvider,
			PromisedConnectionsRepository(),
			RecordingApplicationMessageBus()
		)

		Pair(deferredConnectionProvider, connectionSessionManager)
	}

	private var connectionProvider: LiveServerConnection? = null
	private var isActiveBeforeGettingConnection = false
	private var isActiveAfterGettingConnection = false

	@BeforeAll
	fun before() {
		val (deferredConnectionProvider, connectionSessionManager) = mut

		val futureConnectionProvider =
			connectionSessionManager
				.promiseLibraryConnection(LibraryId(3))
				.toExpiringFuture()

		isActiveBeforeGettingConnection = connectionSessionManager.promiseIsConnectionActive(LibraryId(3)).toExpiringFuture().get() ?: false
		deferredConnectionProvider.sendResolution(expectedConnectionProvider)
		connectionProvider = futureConnectionProvider.get()
		isActiveAfterGettingConnection = connectionSessionManager.promiseIsConnectionActive(LibraryId(3)).toExpiringFuture().get() ?: false
	}

	@Test
	fun `then the connection is not active before getting connection`() {
		assertThat(isActiveBeforeGettingConnection).isFalse
	}

	@Test
	fun `then the connection is correct`() {
		assertThat(connectionProvider).isEqualTo(expectedConnectionProvider)
	}

	@Test
	fun `then the connection is active after getting connection`() {
		assertThat(isActiveAfterGettingConnection).isTrue
	}
}
