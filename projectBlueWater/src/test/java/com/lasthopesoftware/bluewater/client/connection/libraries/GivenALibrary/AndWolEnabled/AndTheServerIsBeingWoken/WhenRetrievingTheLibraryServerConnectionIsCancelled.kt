package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndWolEnabled.AndTheServerIsBeingWoken

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ValidateConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.onEach
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenRetrievingTheLibraryServerConnectionIsCancelled {

	private val deferredConnectionSettings =
		DeferredPromise<ConnectionSettings?>(ConnectionSettings(accessCode = "aB5nf", isWakeOnLanEnabled = true))

	private val deferredLibraryWake = object : DeferredPromise<Unit>(Unit) {
		override fun cancellationRequested() {
			isLibraryServerWakeCancelled = true
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
		every { liveUrlProvider.promiseLiveUrl(LibraryId(3)) } returns Promise.empty() andThen Promise(mockk<ProvideUrls>())

		val libraryConnectionProvider = LibraryConnectionProvider(
			validateConnectionSettings,
			lookupConnection,
			{
				isLibraryServerWakeRequested = true
				deferredLibraryWake
			},
			liveUrlProvider,
			OkHttpFactory,
			AlarmConfiguration(1, Duration.ZERO),
		)

		libraryConnectionProvider
	}

	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: ProvideConnections? = null
	private var isLibraryServerWakeRequested = false
	private var isLibraryServerWakeCancelled = false

	@BeforeAll
	fun act() {
		val futureConnectionProvider =
			mut
				.promiseLibraryConnection(LibraryId(3))
				.onEach(statuses::add)
				.toExpiringFuture()

		deferredConnectionSettings.resolve()

		futureConnectionProvider.cancel(true)

		deferredLibraryWake.resolve()

		connectionProvider = futureConnectionProvider[5, TimeUnit.SECONDS]
	}

	@Test
	fun `then the library awakening is requested`() {
		assertThat(isLibraryServerWakeRequested).isTrue
	}

	@Test
	fun `then the library awakening is cancelled`() {
		assertThat(isLibraryServerWakeCancelled).isTrue
	}

	@Test
	fun `then the connection is null`() {
		assertThat(connectionProvider).isNull()
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.SendingWakeSignal,
				BuildingConnectionStatus.BuildingConnectionFailed,
			)
	}
}
