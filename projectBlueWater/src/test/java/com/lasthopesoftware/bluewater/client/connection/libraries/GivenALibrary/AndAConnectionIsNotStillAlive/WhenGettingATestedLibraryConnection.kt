package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndAConnectionIsNotStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.libraries.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.libraries.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ValidateConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.connection.waking.NoopServerAlarm
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class WhenGettingATestedLibraryConnection {

	companion object {
		private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
		private val firstUrlProvider = Mockito.mock(IUrlProvider::class.java)
		private var connectionProvider: IConnectionProvider? = null
		private var secondConnectionProvider: IConnectionProvider? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val settings = ConnectionSettings(accessCode = "B5nf\"")
			val connectionSettings = mockk<ConnectionSettingsLookup>()
			val deferredSettingsPromise = DeferredPromise(settings)
			val secondDeferredSettingsPromise = DeferredPromise(settings)
			every {
				connectionSettings.lookupConnectionSettings(LibraryId(2))
			} returns deferredSettingsPromise andThen secondDeferredSettingsPromise

			val liveUrlProvider = mockk<ProvideLiveUrl>()
			every { liveUrlProvider.promiseLiveUrl(LibraryId(2)) } returns Promise(firstUrlProvider)

			val testConnections = mockk<TestConnections>()
			every { testConnections.promiseIsConnectionPossible(any()) } returns false.toPromise()

			val validConnections = mockk<ValidateConnectionSettings>()
			every { validConnections.isValid(any()) } returns true

			val libraryConnectionProvider = LibraryConnectionProvider(
				mockk(),
				validConnections,
				connectionSettings,
				NoopServerAlarm(),
				liveUrlProvider,
				testConnections,
				OkHttpFactory.getInstance()
			)

			val libraryId = LibraryId(2)
			val futureConnectionProvider =
				libraryConnectionProvider
					.promiseLibraryConnection(libraryId)
					.updates(statuses::add)
					.toFuture()

			val secondFutureConnectionProvider =
				libraryConnectionProvider
					.promiseTestedLibraryConnection(libraryId)
					.updates(statuses::add)
					.toFuture()

			deferredSettingsPromise.resolve()
			secondDeferredSettingsPromise.resolve()

			connectionProvider = futureConnectionProvider.get()
			secondConnectionProvider = secondFutureConnectionProvider.get()
		}
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(secondConnectionProvider?.urlProvider).isEqualTo(connectionProvider!!.urlProvider)
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete,
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}
}
