package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndWolEnabled.AndTheServerConnectionHasErrors

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenRetrievingTheLibraryConnection {

	private var wakeAttempts = 0
	private var connectionAttempts = 0

	private val mut by lazy {
		val deferredConnectionSettings =
			DeferredPromise<ConnectionSettings?>(ConnectionSettings(accessCode = "aB5nf", isWakeOnLanEnabled = true))

		val libraryConnectionProvider = LibraryConnectionProvider(
			mockk {
				every { isValid(any()) } returns true
			},
			mockk {
				every { lookupConnectionSettings(LibraryId(3)) } returns deferredConnectionSettings
			},
			{
				++wakeAttempts
				Unit.toPromise()
			},
			mockk {
				every { promiseLiveUrl(LibraryId(3)) } answers {
					++connectionAttempts
					Promise(Exception("Let me SLEEP!"))
				}
			},
			OkHttpFactory,
			AlarmConfiguration(5, Duration.millis(100)),
		)

		Pair(deferredConnectionSettings, libraryConnectionProvider)
	}

	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var exception: Exception? = null
	private var runtime = Duration.ZERO

	@BeforeAll
	fun before() {
		val (deferredConnectionSettings, libraryConnectionProvider) = mut

		val futureConnectionProvider =
			libraryConnectionProvider
				.promiseLibraryConnection(LibraryId(3))
				.apply {
					progress.then(statuses::add)
					updates(statuses::add)
				}
				.toExpiringFuture()

		val testStart = DateTime.now()
		deferredConnectionSettings.resolve()
		try {
			futureConnectionProvider.get()
		} catch (ee: ExecutionException) {
			exception = ee.cause as? Exception
		} finally {
		    runtime = Duration(testStart, DateTime.now())
		}
	}

	@Test
	fun `then the wake attempts take the right amount of time`() {
		assertThat(runtime.millis).isGreaterThanOrEqualTo(500L)
	}

	@Test
	fun `then the wake attempts are correct`() {
		assertThat(wakeAttempts).isEqualTo(5)
	}

	@Test
	fun `then the connection attempts are correct`() {
		assertThat(connectionAttempts).isEqualTo(wakeAttempts + 1)
	}

	@Test
	fun `then the exception is propagated`() {
		assertThat(exception?.message).isEqualTo("Let me SLEEP!")
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.SendingWakeSignal,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.SendingWakeSignal,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.SendingWakeSignal,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.SendingWakeSignal,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.SendingWakeSignal,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionFailed,
			)
	}
}
