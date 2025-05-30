package com.lasthopesoftware.bluewater.client.connection.libraries.GivenANullLibrary

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
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRetrievingTheLibraryMediaCenterConnectionDetails {

	private val mut by lazy {
		val deferredConnectionSettings = DeferredPromise(null as ConnectionSettings?)

		val liveUrlProvider = mockk<ProvideLiveServerConnection>()
		every { liveUrlProvider.promiseLiveServerConnection(LibraryId(2)) } returns mockk<LiveServerConnection>().toPromise()

		val libraryConnectionProvider = LibraryConnectionProvider(
            mockk {
				every { promiseConnectionSettings(LibraryId(2)) } returns deferredConnectionSettings
			},
			NoopServerAlarm,
			liveUrlProvider,
			mockk(),
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
				.promiseLibraryConnection(LibraryId(2))
				.onEach(statuses::add)
				.toExpiringFuture()

		deferredConnectionSettings.resolve()
		connectionProvider = futureConnectionProvider.get()
	}

	@Test
	fun `then getting library failed is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.GettingLibraryFailed
			)
	}

	@Test
	fun `then the connection is null`() {
		assertThat(connectionProvider).isNull()
	}
}
