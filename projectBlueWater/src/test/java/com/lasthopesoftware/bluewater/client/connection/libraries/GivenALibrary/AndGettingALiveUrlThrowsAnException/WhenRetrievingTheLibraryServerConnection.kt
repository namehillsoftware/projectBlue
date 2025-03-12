package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndGettingALiveUrlThrowsAnException

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ValidateConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import com.lasthopesoftware.bluewater.client.connection.waking.NoopServerAlarm
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.onEach
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenRetrievingTheLibraryServerConnection {
	private val connectionSettings = ConnectionSettings(accessCode = "aB5nf")
	private val deferredConnectionSettings = DeferredPromise<ConnectionSettings?>(connectionSettings)
	private val deferredUrlPromise = DeferredPromise<ProvideUrls?>(IOException())

	private val mut by lazy {
		val validateConnectionSettings = mockk<ValidateConnectionSettings>()
		every { validateConnectionSettings.isValid(connectionSettings) } returns true

		val lookupConnection = mockk<LookupConnectionSettings>()
		every {
			lookupConnection.lookupConnectionSettings(LibraryId(2))
		} returns deferredConnectionSettings

		val liveUrlProvider = mockk<ProvideLiveUrl>()
		every { liveUrlProvider.promiseLiveUrl(LibraryId(2)) } returns deferredUrlPromise

		val libraryConnectionProvider = LibraryConnectionProvider(
			validateConnectionSettings,
			lookupConnection,
			NoopServerAlarm,
			liveUrlProvider,
			OkHttpFactory,
			mockk(),
		)

		libraryConnectionProvider
	}

	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: ProvideConnections? = null
	private var exception: IOException? = null

	@BeforeAll
	fun before() {
		val futureConnectionProvider =
			mut.promiseLibraryConnection(LibraryId(2))
				.onEach(statuses::add)
				.toExpiringFuture()

		deferredConnectionSettings.resolve()
		deferredUrlPromise.resolve()
		try {
			connectionProvider = futureConnectionProvider[30, TimeUnit.SECONDS]
		} catch (e: ExecutionException) {
			if (e.cause is IOException) exception = e.cause as IOException?
		} catch (e: TimeoutException) {
			if (e.cause is IOException) exception = e.cause as IOException?
		}
	}

	@Test
	fun `then a connection provider is not returned`() {
		assertThat(connectionProvider).isNull()
	}

	@Test
	fun `then an IOException is returned`() {
		assertThat(exception).isNotNull
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionFailed
			)
	}
}
