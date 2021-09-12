package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndWolEnabled.AndTheServerIsBeingWoken

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
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

class WhenRetrievingTheLibraryConnectionIsCancelled {

	companion object {
		private val urlProvider = mockk<IUrlProvider>()
		private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
		private var connectionProvider: IConnectionProvider? = null
		private var isLibraryServerWakeRequested = false
		private var isLibraryServerWakeCancelled = false

		@BeforeClass
		@JvmStatic
		fun before() {
			val validateConnectionSettings = mockk<ValidateConnectionSettings>()
			every { validateConnectionSettings.isValid(any()) } returns true

			val deferredConnectionSettings = DeferredPromise<ConnectionSettings?>(ConnectionSettings(accessCode = "aB5nf", isWakeOnLanEnabled = true))
			val lookupConnection = mockk<LookupConnectionSettings>()
			every {
				lookupConnection.lookupConnectionSettings(LibraryId(3))
			} returns deferredConnectionSettings

			val liveUrlProvider = mockk<ProvideLiveUrl>()
			every { liveUrlProvider.promiseLiveUrl(LibraryId(3)) } returns Promise(urlProvider)

			val deferredLibraryWake = object : DeferredPromise<Unit>(Unit) {
				override fun run() {
					isLibraryServerWakeCancelled = true
				}
			}

			val libraryConnectionProvider = LibraryConnectionProvider(
                validateConnectionSettings,
                lookupConnection,
                {
                    isLibraryServerWakeRequested = true
					deferredLibraryWake
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

			deferredConnectionSettings.resolve()

			futureConnectionProvider.cancel(true)

			deferredLibraryWake.resolve()

			connectionProvider = futureConnectionProvider[5, TimeUnit.SECONDS]
		}
	}

	@Test
	fun thenTheLibraryAwakeningIsRequested() {
		assertThat(isLibraryServerWakeRequested).isTrue
	}

	@Test
	fun thenTheLibraryAwakeningIsCancelled() {
		assertThat(isLibraryServerWakeCancelled).isTrue
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
				BuildingConnectionStatus.SendingWakeSignal,
				BuildingConnectionStatus.BuildingConnectionFailed,
			)
	}
}
