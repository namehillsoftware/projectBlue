package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndWolEnabled

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.ProvideLiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.onEach
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRetrievingTheLibraryServerConnectionIsCancelled {

	private val mut by lazy {
		val deferredConnectionSettings =
			DeferredPromise<MediaCenterConnectionSettings?>(MediaCenterConnectionSettings(accessCode = "aB5nf", isWakeOnLanEnabled = true))

		val liveUrlProvider = mockk<ProvideLiveServerConnection>()
		every { liveUrlProvider.promiseLiveServerConnection(LibraryId(3)) } returns Promise(mockk<LiveServerConnection>())

		val libraryConnectionProvider = LibraryConnectionProvider(
            mockk {
				every { promiseConnectionSettings(LibraryId(3)) } returns deferredConnectionSettings
			},
			{
				isLibraryServerWoken = true
				Unit.toPromise()
			},
			liveUrlProvider,
			AlarmConfiguration(0, Duration.ZERO),
		)

		Pair(deferredConnectionSettings, libraryConnectionProvider)
	}

	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: LiveServerConnection? = null
	private var isLibraryServerWoken = false

	@BeforeAll
	fun before() {
		val (deferredConnectionSettings, libraryConnectionProvider) = mut

		val futureConnectionProvider =
			libraryConnectionProvider
				.promiseLibraryConnection(LibraryId(3))
				.onEach(statuses::add)
				.toExpiringFuture()

		futureConnectionProvider.cancel(true)

		deferredConnectionSettings.resolve()
		connectionProvider = futureConnectionProvider.get()
	}

	@Test
	fun `then the library is not woken`() {
		assertThat(isLibraryServerWoken).isFalse
	}

	@Test
	fun `then the connection is null`() {
		assertThat(connectionProvider).isNull()
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnectionFailed,
			)
	}
}
