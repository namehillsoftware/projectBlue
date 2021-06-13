package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.libraries.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.libraries.ValidateConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.connection.waking.NoopServerAlarm
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class WhenRetrievingTheLibraryConnection {
	@Test
	fun thenTheConnectionIsNotActiveBeforeGettingConnection() {
		assertThat(isActiveBeforeGettingConnection).isFalse
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(connectionProvider?.urlProvider).isEqualTo(urlProvider)
	}

	@Test
	fun thenTheConnectionIsActiveAfterGettingConnection() {
		assertThat(isActiveAfterGettingConnection).isTrue
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}

	companion object {
		private val urlProvider = Mockito.mock(IUrlProvider::class.java)
		private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
		private var connectionProvider: IConnectionProvider? = null
		private var isActiveBeforeGettingConnection = false
		private var isActiveAfterGettingConnection = false

		@BeforeClass
		@JvmStatic
		fun before() {
			val validateConnectionSettings = mockk<ValidateConnectionSettings>()
			every { validateConnectionSettings.isValid(any()) } returns true

			val deferredConnectionSettings = DeferredPromise(ConnectionSettings(accessCode = "aB5nf"))

			val lookupConnection = mockk<LookupConnectionSettings>()
			every {
				lookupConnection.lookupConnectionSettings(LibraryId(3))
			} returns deferredConnectionSettings

			val liveUrlProvider = mockk<ProvideLiveUrl>()
			every { liveUrlProvider.promiseLiveUrl(LibraryId(3)) } returns urlProvider.toPromise()

			val libraryConnectionProvider = LibraryConnectionProvider(
				mockk(),
				validateConnectionSettings,
				lookupConnection,
				NoopServerAlarm(),
				liveUrlProvider,
				Mockito.mock(TestConnections::class.java),
				OkHttpFactory.getInstance()
			)

			val futureConnectionProvider =
				libraryConnectionProvider
					.promiseLibraryConnection(LibraryId(3))
					.updates(statuses::add)
					.toFuture()

			isActiveBeforeGettingConnection = libraryConnectionProvider.isConnectionActive(LibraryId(3))
			deferredConnectionSettings.resolve()
			connectionProvider = futureConnectionProvider.get()
			isActiveAfterGettingConnection = libraryConnectionProvider.isConnectionActive(LibraryId(3))
		}
	}
}
