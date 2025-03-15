package com.lasthopesoftware.bluewater.client.connection.builder.GivenSecureServerIsFoundViaLookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.builder.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class WhenCancellingTheUrlScan {

	private val promisedUrl by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(35)) } returns Promise(
			ServerInfo(
				143,
				452,
				"1.2.3.4",
				emptySet(),
				emptySet(),
				Hex.decodeHex("2386166660562C5AAA1253B2BED7C2483F9C2D45")
			)
		)

		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(35)) } returns ConnectionSettings(accessCode = "gooPc").toPromise()

		val urlScanner = UrlScanner(
			PassThroughBase64Encoder,
			serverLookup,
			connectionSettingsLookup,
			mockk {
				every {
					getServerClient(match { a ->
						listOf(
							"https://1.2.3.4:452/MCWS/v1/",
							"http://1.2.3.4:143/MCWS/v1/"
						).contains(a.baseUrl.toString())
					})
				} answers {
					val urlProvider = firstArg<ServerConnection>()
					mockk {
						every { promiseResponse(URL(urlProvider.baseUrl, "Alive")) } returns Promise(
							CancellationException("Maybe later!")
						)
					}
				}
			}
		)

		val urlScan = urlScanner.promiseBuiltUrlProvider(LibraryId(35))

		urlScan.cancel()

		urlScan.toExpiringFuture()[5, TimeUnit.SECONDS]
	}

	@Test
	fun `then a null URL is returned`() {
		assertThat(promisedUrl).isNull()
	}
}
