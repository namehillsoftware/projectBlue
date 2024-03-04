package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndWolDisabled.AndTheLiveUrlIsBeingRetrieved

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
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRetrievingTheLibraryConnectionIsCancelled {

	private val urlProvider = mockk<IUrlProvider>()
	private val deferredConnectionSettings = DeferredPromise<ConnectionSettings?>(
		ConnectionSettings(
			accessCode = "aB5nf",
			isWakeOnLanEnabled = false
		)
	)
	private val deferredLiveUrlProvider = object : DeferredPromise<IUrlProvider?>(urlProvider) {
		override fun cancellationRequested() {
			isLiveUrlProviderCancelled = true
			resolve(null)
		}
	}

	private val mut by lazy {
		val validateConnectionSettings = mockk<ValidateConnectionSettings>()
		every { validateConnectionSettings.isValid(any()) } returns true

		val lookupConnection = mockk<LookupConnectionSettings>()
		every {
			lookupConnection.lookupConnectionSettings(LibraryId(3))
		} returns deferredConnectionSettings
		val liveUrlProvider = mockk<ProvideLiveUrl>()
		every { liveUrlProvider.promiseLiveUrl(LibraryId(3)) } returns deferredLiveUrlProvider

		val libraryConnectionProvider = LibraryConnectionProvider(
			validateConnectionSettings,
			lookupConnection,
			{
				isLibraryServerWoken = true
				Unit.toPromise()
			},
			liveUrlProvider,
			OkHttpFactory
		)

		libraryConnectionProvider
	}

	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: IConnectionProvider? = null
	private var isLibraryServerWoken = false
	private var isLiveUrlProviderCancelled = false

	@BeforeAll
	fun act() {
		val futureConnectionProvider =
			mut.promiseLibraryConnection(LibraryId(3))
				.apply {
					progress.then(statuses::add)
					updates(statuses::add)
				}
				.toExpiringFuture()

		deferredConnectionSettings.resolve()

		futureConnectionProvider.cancel(true)

		deferredLiveUrlProvider.resolve()

		connectionProvider = futureConnectionProvider.get()
	}

	@Test
	fun `then the library is not woken`() {
		assertThat(isLibraryServerWoken).isFalse
	}

	@Test
	fun `then the connection is null`() {
		assertThat(connectionProvider).isNull()
	}

	@Test
	fun `then the live url is cancelled`() {
		assertThat(isLiveUrlProviderCancelled).isTrue
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionFailed,
			)
	}
}
