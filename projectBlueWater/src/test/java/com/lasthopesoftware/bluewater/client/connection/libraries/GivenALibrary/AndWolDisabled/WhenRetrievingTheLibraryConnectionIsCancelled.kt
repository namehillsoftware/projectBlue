package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndWolDisabled

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
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class WhenRetrievingTheLibraryConnectionIsCancelled {

	companion object {
		private val urlProvider = mockk<IUrlProvider>()
		private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
		private var connectionProvider: IConnectionProvider? = null
		private var isLibraryServerWoken = false

		@BeforeClass
		@JvmStatic
		fun before() {
			val validateConnectionSettings = mockk<ValidateConnectionSettings>()
			every { validateConnectionSettings.isValid(any()) } returns true

			val deferredConnectionSettings = DeferredPromise(ConnectionSettings(accessCode = "aB5nf", isWakeOnLanEnabled = false))

			val lookupConnection = mockk<LookupConnectionSettings>()
			every {
				lookupConnection.lookupConnectionSettings(LibraryId(3))
			} returns deferredConnectionSettings

			val liveUrlProvider = mockk<ProvideLiveUrl>()
			every { liveUrlProvider.promiseLiveUrl(LibraryId(3)) } returns Promise(urlProvider)

			val libraryConnectionProvider = LibraryConnectionProvider(
                validateConnectionSettings,
                lookupConnection,
                {
                    isLibraryServerWoken = true
                    Unit.toPromise()
                },
                liveUrlProvider,
                OkHttpFactory.getInstance()
            )

			val futureConnectionProvider =
				libraryConnectionProvider
					.promiseLibraryConnection(LibraryId(3))
					.apply {
						progress.then(statuses::add)
						updates(statuses::add)
					}
					.toFuture()

			futureConnectionProvider.cancel(true)

			deferredConnectionSettings.resolve()
			connectionProvider = futureConnectionProvider.get()
		}
	}

	@Test
	fun thenTheLibraryIsNotWoken() {
		assertThat(isLibraryServerWoken).isFalse
	}

	@Test
	fun thenTheConnectionIsNull() {
		assertThat(connectionProvider).isNull()
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnectionFailed,
			)
	}
}
