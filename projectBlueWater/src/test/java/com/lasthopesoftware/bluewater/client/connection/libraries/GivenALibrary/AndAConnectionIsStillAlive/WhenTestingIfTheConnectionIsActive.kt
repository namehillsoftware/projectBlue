package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndAConnectionIsStillAlive

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
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenTestingIfTheConnectionIsActive {
	@Test
	fun thenTheConnectionIsActive() {
		assertThat(isActive).isTrue
	}

	companion object {
		private val firstUrlProvider = mockk<IUrlProvider>()
		private var isActive: Boolean? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val validateConnectionSettings = mockk<ValidateConnectionSettings>()
			every { validateConnectionSettings.isValid(any()) } returns true

			val deferredConnectionSettings = DeferredPromise(ConnectionSettings(accessCode = "aB5nf"))

			val lookupConnection = mockk<LookupConnectionSettings>()
			every { lookupConnection.lookupConnectionSettings(LibraryId(2)) } returns deferredConnectionSettings

			val liveUrlProvider = mockk<ProvideLiveUrl>()
			every  { liveUrlProvider.promiseLiveUrl(LibraryId(2)) } returns Promise(firstUrlProvider)

			val connectionsTester = mockk<TestConnections>()
			every  { connectionsTester.promiseIsConnectionPossible(any()) } returns true.toPromise()

			val libraryConnectionProvider = LibraryConnectionProvider(
				mockk(),
				mockk(),
				lookupConnection,
				NoopServerAlarm(),
				liveUrlProvider,
				connectionsTester,
				OkHttpFactory.getInstance()
			)
			val libraryId = LibraryId(2)
			val futureConnectionProvider = libraryConnectionProvider.promiseLibraryConnection(libraryId).toFuture()

			deferredConnectionSettings.resolve()

			futureConnectionProvider[30, TimeUnit.SECONDS]
			isActive = libraryConnectionProvider.isConnectionActive(libraryId)
		}
	}
}
