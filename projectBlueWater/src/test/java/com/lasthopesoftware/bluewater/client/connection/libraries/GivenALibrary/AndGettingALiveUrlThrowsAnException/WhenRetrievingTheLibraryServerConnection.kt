package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndGettingALiveUrlThrowsAnException

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.ProvideLiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
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
	private val connectionSettings = MediaCenterConnectionSettings(accessCode = "aB5nf")
	private val deferredConnectionSettings = DeferredPromise<MediaCenterConnectionSettings?>(connectionSettings)
	private val deferredUrlPromise = DeferredPromise<LiveServerConnection?>(IOException())

	private val mut by lazy {
		val liveUrlProvider = mockk<ProvideLiveServerConnection>()
		every { liveUrlProvider.promiseLiveServerConnection(LibraryId(2)) } returns deferredUrlPromise

		val libraryConnectionProvider = LibraryConnectionProvider(
			mockk {
				every { promiseConnectionSettings(LibraryId(2)) } returns deferredConnectionSettings
			},
			NoopServerAlarm,
			liveUrlProvider,
			mockk(),
		)

		libraryConnectionProvider
	}

	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: LiveServerConnection? = null
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
