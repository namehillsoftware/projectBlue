package com.lasthopesoftware.bluewater.client.connection.builder.GivenAnAccessCodeThatIsAValidServer

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
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

class WhenCancellingTheUrlScan {

	companion object {
		private val cancellationException by lazy {
			val connectionTester = mockk<TestConnections>()
			every { connectionTester.promiseIsConnectionPossible(match { a -> a.urlProvider.baseUrl.toString() == "http://gooPc:80/MCWS/v1/" }) } returns Promise { m ->
				m.cancellationRequested {
					m.sendRejection(CancellationException("Bye now!"))
				}
			}

			val connectionSettingsLookup = mockk<LookupConnectionSettings>()
			every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(14)) } returns ConnectionSettings(accessCode = "http://gooPc:80").toPromise()

			val urlScanner = UrlScanner(
				PassThroughBase64Encoder,
				connectionTester,
				mockk(),
				connectionSettingsLookup,
				OkHttpFactory
            )

			val promisedScan = urlScanner.promiseBuiltUrlProvider(LibraryId(14))
			promisedScan.cancel()

			try {
				promisedScan.toFuture()[5, TimeUnit.SECONDS]
				null
			} catch (ee: ExecutionException) {
				ee.cause as? CancellationException
			}
		}
	}

	@Test
	fun thenTheCancellationExceptionIsReturned() {
		assertThat(cancellationException?.message).isEqualTo("Bye now!")
	}
}
