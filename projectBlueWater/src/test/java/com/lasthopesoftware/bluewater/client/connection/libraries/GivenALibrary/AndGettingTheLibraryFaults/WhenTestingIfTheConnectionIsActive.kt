package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndGettingTheLibraryFaults

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
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
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenTestingIfTheConnectionIsActive {
	@Test
	fun thenTheConnectionIsNotActive() {
		Assertions.assertThat(isActive).isFalse
	}

	@Test
	fun thenAnIOExceptionIsReturned() {
		Assertions.assertThat(exception).isNotNull
	}

	companion object {
		private val urlProvider = Mockito.mock(IUrlProvider::class.java)
		private var exception: IOException? = null
		private var isActive = false
		@BeforeClass
		@JvmStatic
		fun before() {
			val validateConnectionSettings = mockk<ValidateConnectionSettings>()
			every { validateConnectionSettings.isValid(any()) } returns true

			val deferredConnectionSettings = DeferredPromise<ConnectionSettings>(IOException("OMG"))

			val lookupConnection = mockk<LookupConnectionSettings>()
			every {
				lookupConnection.lookupConnectionSettings(LibraryId(2))
			} returns deferredConnectionSettings

			val liveUrlProvider = mockk<ProvideLiveUrl>()
			every { liveUrlProvider.promiseLiveUrl(LibraryId(2)) } returns Promise(urlProvider)

			val libraryConnectionProvider = LibraryConnectionProvider(
                validateConnectionSettings,
                lookupConnection,
                NoopServerAlarm(),
                liveUrlProvider,
                Mockito.mock(TestConnections::class.java),
                OkHttpFactory.getInstance()
            )

			val futureConnectionProvider =
				libraryConnectionProvider
					.promiseLibraryConnection(LibraryId(2))
					.toFuture()

			deferredConnectionSettings.resolve()
			try {
				futureConnectionProvider[30, TimeUnit.SECONDS]
			} catch (e: ExecutionException) {
				if (e.cause is IOException) {
					exception = e.cause as IOException?
				}
			} catch (e: TimeoutException) {
				if (e.cause is IOException) {
					exception = e.cause as IOException?
				}
			}
			isActive = libraryConnectionProvider.isConnectionActive(LibraryId(2))
		}
	}
}
