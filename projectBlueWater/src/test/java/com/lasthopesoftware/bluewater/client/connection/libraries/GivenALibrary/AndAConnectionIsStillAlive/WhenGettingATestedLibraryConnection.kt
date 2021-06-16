package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndAConnectionIsStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ValidateConnectionSettings
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
import java.util.concurrent.TimeUnit

class WhenGettingATestedLibraryConnection {
	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(secondConnectionProvider).isEqualTo(connectionProvider!!)
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
		private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
		private val firstUrlProvider = Mockito.mock(IUrlProvider::class.java)
		private var connectionProvider: IConnectionProvider? = null
		private var secondConnectionProvider: IConnectionProvider? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionSettings = ConnectionSettings(accessCode = "aB5nf")
			val deferredConnectionSettings = DeferredPromise(connectionSettings)
			val connectionSettingsLookup = mockk<LookupConnectionSettings>()
			every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(2)) } returns deferredConnectionSettings

			val liveUrlProvider = mockk<ProvideLiveUrl>()
			every { liveUrlProvider.promiseLiveUrl(LibraryId(2)) } returns firstUrlProvider.toPromise()

			val connectionsTester = mockk<TestConnections>()
			every { connectionsTester.promiseIsConnectionPossible(any()) } returns true.toPromise()

			val validConnections = mockk<ValidateConnectionSettings>()
			every { validConnections.isValid(any()) } returns true

			val libraryConnectionProvider = LibraryConnectionProvider(
				mockk(),
				validConnections,
				connectionSettingsLookup,
				NoopServerAlarm(),
				liveUrlProvider,
				connectionsTester,
				OkHttpFactory.getInstance()
			)

			val libraryId = LibraryId(2)

			val futureConnectionProvider =
				libraryConnectionProvider
					.promiseLibraryConnection(libraryId)
					.updates(statuses::add)
					.toFuture()

			deferredConnectionSettings.resolve()

			connectionProvider = futureConnectionProvider[30, TimeUnit.SECONDS]

			secondConnectionProvider =
				libraryConnectionProvider
					.promiseTestedLibraryConnection(libraryId)
					.updates(statuses::add)
					.toFuture()
					.get()
		}
	}
}
