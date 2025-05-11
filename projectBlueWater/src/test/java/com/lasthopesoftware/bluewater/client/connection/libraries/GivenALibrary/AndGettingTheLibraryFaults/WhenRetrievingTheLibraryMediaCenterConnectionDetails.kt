package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndGettingTheLibraryFaults

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.ProvideLiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.waking.NoopServerAlarm
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.onEach
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenRetrievingTheLibraryMediaCenterConnectionDetails {

	private val serverConnection = mockk<LiveServerConnection>()
	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: LiveServerConnection? = null
	private var exception: IOException? = null

	@BeforeAll
	fun before() {
		val deferredConnectionSettings = DeferredPromise<ConnectionSettings?>(IOException("OMG"))

		val liveUrlProvider = mockk<ProvideLiveServerConnection>()
		every { liveUrlProvider.promiseLiveServerConnection(LibraryId(2)) } returns Promise(serverConnection)

		val libraryConnectionProvider = LibraryConnectionProvider(
            mockk {
				every { promiseConnectionSettings(LibraryId(2)) } returns deferredConnectionSettings
			},
			NoopServerAlarm,
			liveUrlProvider,
			mockk(),
		)

		val futureConnectionProvider =
			libraryConnectionProvider
				.promiseLibraryConnection(LibraryId(2))
				.onEach(statuses::add)
				.toExpiringFuture()

		deferredConnectionSettings.resolve()
		try {
			connectionProvider = futureConnectionProvider[30, TimeUnit.SECONDS]
		} catch (e: ExecutionException) {
			exception = e.cause as? IOException ?: throw e
		}
	}

	@Test
	fun `then a connection provider is not returned`() {
		assertThat(connectionProvider).isNull()
	}

	@Test
	fun `then an ioException is returned`() {
		assertThat(exception).isNotNull
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.GettingLibraryFailed
			)
	}
}
