package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndASecureServerIsFoundViaLookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class `When Getting The Live Connection` {

	private val serverConnection by lazy {
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

		val urlScanner = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			connectionSettingsLookup,
			mockk {
				every {
					getServerClient(match { a ->
						listOf(
							"https://1.2.3.4:452",
							"http://1.2.3.4:143"
						).contains(a.baseUrl.toString())
					})
				} answers {
					val urlProvider = firstArg<ServerConnection>()
					mockk {
						every { promiseResponse(URL(urlProvider.baseUrl, "MCWS/v1/Alive")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"Ok",
								"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
									<Response Status="OK"></Response>""".toByteArray().inputStream()
							)
						)
					}
				}
			},
			mockk(),
		)

		urlScanner.promiseLiveServerConnection(LibraryId(35)).toExpiringFuture().get()
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(serverConnection?.serverConnection?.baseUrl.toString()).isEqualTo("https://1.2.3.4:452")
	}

	@Test
	fun `then the certificate fingerprint is correct`() {
		assertThat(serverConnection?.serverConnection?.certificateFingerprint)
			.isEqualTo(Hex.decodeHex("2386166660562C5AAA1253B2BED7C2483F9C2D45"))
	}
}
