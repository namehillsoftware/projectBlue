package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.settings.ValidateConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRetrievingTheLibraryConnection {

	private val expectedConnectionProvider = mockk<ProvideConnections>()

	private val mut by lazy {
		val validateConnectionSettings = mockk<ValidateConnectionSettings>()
		every { validateConnectionSettings.isValid(any()) } returns true

		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, ProvideConnections?>()

		val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
		every { libraryConnectionProvider.promiseLibraryConnection(LibraryId(3)) } returns deferredConnectionProvider

		val connectionSessionManager = ConnectionSessionManager(
			mockk(),
			libraryConnectionProvider,
			PromisedConnectionsRepository(),
			RecordingApplicationMessageBus()
		)

		Pair(deferredConnectionProvider, connectionSessionManager)
	}

	private var connectionProvider: ProvideConnections? = null
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
