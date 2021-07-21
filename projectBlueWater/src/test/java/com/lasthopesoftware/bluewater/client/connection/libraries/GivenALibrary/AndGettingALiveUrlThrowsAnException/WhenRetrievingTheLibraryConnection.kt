package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndGettingALiveUrlThrowsAnException

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
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenRetrievingTheLibraryConnection {
	@Test
	fun thenAConnectionProviderIsNotReturned() {
		AssertionsForClassTypes.assertThat(connectionProvider).isNull()
	}

	@Test
	fun thenAnIOExceptionIsReturned() {
		AssertionsForClassTypes.assertThat(exception).isNotNull
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		Assertions.assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionFailed
			)
	}

	companion object {
		private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
		private var connectionProvider: IConnectionProvider? = null
		private var exception: IOException? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionSettings = ConnectionSettings(accessCode = "aB5nf")

			val validateConnectionSettings = mockk<ValidateConnectionSettings>()
			every { validateConnectionSettings.isValid(connectionSettings) } returns true

			val deferredConnectionSettings = DeferredPromise(connectionSettings)

			val lookupConnection = mockk<LookupConnectionSettings>()
			every {
				lookupConnection.lookupConnectionSettings(LibraryId(2))
			} returns deferredConnectionSettings

			val liveUrlProvider = mockk<ProvideLiveUrl>()
			val deferredUrlPromise = DeferredPromise<IUrlProvider?>(IOException())
			every { liveUrlProvider.promiseLiveUrl(LibraryId(2)) } returns deferredUrlPromise

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

			deferredConnectionSettings.resolve()
			deferredUrlPromise.resolve()
			try {
				connectionProvider = futureConnectionProvider[30, TimeUnit.SECONDS]
			} catch (e: ExecutionException) {
				if (e.cause is IOException) exception = e.cause as IOException?
			} catch (e: TimeoutException) {
				if (e.cause is IOException) exception = e.cause as IOException?
			}
		}
	}
}
