package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndASubsonicConnection.AndASecureServerIsFoundViaLookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
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

class `When Cancelling During Connection Test` {

	private val promisedUrl by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(35)) } returns Promise(
			ServerInfo(
				null,
				461,
				setOf("LD1kt8LI7aC"),
				emptySet(),
				emptySet(),
				Hex.decodeHex("3e29389b9409419180eae3159dbb1ecc")
			)
		)

		val urlScanner = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			mockk {
				every { promiseConnectionSettings(LibraryId(35)) } returns SubsonicConnectionSettings(
					url = "5Tyzhomsuwt",
					userName = "",
					password = "",
				).toPromise()
			},
			mockk {
				every {
					getServerClient(match<SubsonicConnectionDetails> { a ->
						a.baseUrl.toString() == "https://LD1kt8LI7aC:461"
					})
				} answers {
					val urlProvider = firstArg<SubsonicConnectionDetails>()
					mockk {
						every { promiseResponse(URL(urlProvider.baseUrl, "rest/ping.view")) } returns Promise(
							CancellationException("Maybe later!")
						)
					}
				}
			},
			mockk(),
		)

		val urlScan = urlScanner.promiseLiveServerConnection(LibraryId(35))

		urlScan.cancel()

		urlScan.toExpiringFuture()[5, TimeUnit.SECONDS]
	}

	@Test
	fun `then a null URL is returned`() {
		assertThat(promisedUrl).isNull()
	}
}
