package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndGettingTheLibraryFaults

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ValidateConnectionSettings
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
import java.net.URL
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenRetrievingTheLibraryServerConnection {

	private val serverConnection = ServerConnection(URL("http://test"))
	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: ProvideConnections? = null
	private var exception: IOException? = null

	@BeforeAll
	fun before() {
		val validateConnectionSettings = mockk<ValidateConnectionSettings>()
		every { validateConnectionSettings.isValid(any()) } returns true

		val deferredConnectionSettings = DeferredPromise<ConnectionSettings?>(IOException("OMG"))

		val lookupConnection = mockk<LookupConnectionSettings>()
		every {
			lookupConnection.lookupConnectionSettings(LibraryId(2))
		} returns deferredConnectionSettings

		val liveUrlProvider = mockk<ProvideLiveServerConnection>()
		every { liveUrlProvider.promiseLiveServerConnection(LibraryId(2)) } returns Promise(serverConnection)

		val libraryConnectionProvider = LibraryConnectionProvider(
			validateConnectionSettings,
			lookupConnection,
			NoopServerAlarm,
			liveUrlProvider,
			OkHttpFactory,
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
