package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ValidateConnectionSettings
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

class WhenRetrievingTheLibraryConnectionTwice {
	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(secondConnectionProvider).isEqualTo(connectionProvider)
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
		private val firstUrlProvider = Mockito.mock(IUrlProvider::class.java)
		private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
		private var connectionProvider: IConnectionProvider? = null
		private var secondConnectionProvider: IConnectionProvider? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val validateConnectionSettings = mockk<ValidateConnectionSettings>()
			every { validateConnectionSettings.isValid(any()) } returns true

			val deferredConnectionSettings = DeferredPromise(ConnectionSettings(accessCode = "aB5nf"))

			val lookupConnection = mockk<LookupConnectionSettings>()
			every {
				lookupConnection.lookupConnectionSettings(LibraryId(2))
			} returns deferredConnectionSettings

			val liveUrlProvider = mockk<ProvideLiveUrl>()
			every { liveUrlProvider.promiseLiveUrl(LibraryId(2)) } returns firstUrlProvider.toPromise()

			val libraryConnectionProvider = LibraryConnectionProvider(
				validateConnectionSettings,
				lookupConnection,
				NoopServerAlarm(),
				liveUrlProvider,
				OkHttpFactory.getInstance()
			)

			val futureConnectionProvider =
				libraryConnectionProvider
					.promiseLibraryConnection(LibraryId(2))
					.updates(statuses::add)
					.toFuture()

			val secondFutureConnectionProvider =
				libraryConnectionProvider
					.promiseLibraryConnection(LibraryId(2))
					.updates(statuses::add)
					.toFuture()
			deferredConnectionSettings.resolve()
			connectionProvider = futureConnectionProvider.get()
			secondConnectionProvider = secondFutureConnectionProvider.get()
		}
	}
}
