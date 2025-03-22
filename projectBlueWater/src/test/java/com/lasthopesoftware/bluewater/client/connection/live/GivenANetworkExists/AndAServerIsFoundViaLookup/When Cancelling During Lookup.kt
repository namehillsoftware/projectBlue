package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndAServerIsFoundViaLookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class `When Cancelling During Lookup` {
	private val cancellationException by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(55)) } returns Promise { m ->
			m.awaitCancellation {
				m.sendRejection(CancellationException("Yup I'm cancelled"))
			}
		}

		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(55)) } returns ConnectionSettings(accessCode = "gooPc").toPromise()

		val connectionProvider = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			connectionSettingsLookup,
			mockk(),
			mockk(),
		)

		val promisedConnection = connectionProvider.promiseLiveServerConnection(LibraryId(55))
		promisedConnection.cancel()

		try {
			promisedConnection.toExpiringFuture()[5, TimeUnit.SECONDS]
			null
		} catch (ee: ExecutionException) {
			ee.cause as? CancellationException
		}
	}

	@Test
	fun thenTheLookupIsCancelled() {
		assertThat(cancellationException?.message).isEqualTo("Yup I'm cancelled")
	}
}
