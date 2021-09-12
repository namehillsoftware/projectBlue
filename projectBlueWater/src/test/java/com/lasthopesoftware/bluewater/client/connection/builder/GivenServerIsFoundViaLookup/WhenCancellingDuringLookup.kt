package com.lasthopesoftware.bluewater.client.connection.builder.GivenServerIsFoundViaLookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenCancellingDuringLookup {
	companion object {
		private val cancellationException by lazy {
			val connectionTester = mockk<TestConnections>()
			every { connectionTester.promiseIsConnectionPossible(any()) } returns false.toPromise()
			every { connectionTester.promiseIsConnectionPossible(match { a ->
				"http://1.2.3.4:143/MCWS/v1/" == a.urlProvider.baseUrl.toString()
			}) } returns Promise { m ->
				m.cancellationRequested {
					m.sendRejection(CancellationException("I'm not supposed to be cancelled"))
				}
			}

			val serverLookup = mockk<LookupServers>()
			every { serverLookup.promiseServerInformation(LibraryId(55)) } returns Promise { m ->
				m.cancellationRequested {
					m.sendRejection(CancellationException("Yup I'm cancelled"))
				}
			}

			val connectionSettingsLookup = mockk<LookupConnectionSettings>()
			every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(55)) } returns ConnectionSettings(accessCode = "gooPc").toPromise()

			val urlScanner = UrlScanner(
				PassThroughBase64Encoder,
				connectionTester,
				serverLookup,
				connectionSettingsLookup,
				OkHttpFactory.getInstance()
			)

			val promisedUrl = urlScanner.promiseBuiltUrlProvider(LibraryId(55))
			promisedUrl.cancel()

			try {
				promisedUrl.toFuture()[5, TimeUnit.SECONDS]
				null
			} catch (ee: ExecutionException) {
				ee.cause as? CancellationException
			}
		}
	}

	@Test
	fun thenTheLookupIsCancelled() {
		assertThat(cancellationException?.message).isEqualTo("Yup I'm cancelled")
	}
}
