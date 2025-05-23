package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndWolEnabled.AndTheServerIsNotAwakeAfterTwoAttempts

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.onEach
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRetrievingTheLibraryMediaCenterConnectionDetails {

	private var wakeAttempts = 0
	private var connectionAttempts = 0

	private val mut by lazy {
		val deferredConnectionSettings =
			DeferredPromise<ConnectionSettings?>(MediaCenterConnectionSettings(accessCode = "aB5nf", isWakeOnLanEnabled = true))

		val libraryConnectionProvider = LibraryConnectionProvider(
            mockk {
				every { promiseConnectionSettings(LibraryId(3)) } returns deferredConnectionSettings
			},
			{
				++wakeAttempts
				Unit.toPromise()
			},
			mockk {
				every { promiseLiveServerConnection(LibraryId(3)) } answers {
					++connectionAttempts
					null.toPromise()
				}
			},
			AlarmConfiguration(2, Duration.millis(500)),
		)

		Pair(deferredConnectionSettings, libraryConnectionProvider)
	}

	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: LiveServerConnection? = null

	@BeforeAll
	fun before() {
		val (deferredConnectionSettings, libraryConnectionProvider) = mut

		val futureConnectionProvider =
			libraryConnectionProvider
				.promiseLibraryConnection(LibraryId(3))
				.onEach(statuses::add)
				.toExpiringFuture()

		deferredConnectionSettings.resolve()
		connectionProvider = futureConnectionProvider.get()
	}

	@Test
	fun `then the wake attempts are correct`() {
		assertThat(wakeAttempts).isEqualTo(2)
	}

	@Test
	fun `then the connection attempts are correct`() {
		assertThat(connectionAttempts).isEqualTo(wakeAttempts + 1)
	}

	@Test
	fun `then the connection is correct`() {
		assertThat(connectionProvider).isNull()
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.SendingWakeSignal,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.SendingWakeSignal,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionFailed,
			)
	}
}
