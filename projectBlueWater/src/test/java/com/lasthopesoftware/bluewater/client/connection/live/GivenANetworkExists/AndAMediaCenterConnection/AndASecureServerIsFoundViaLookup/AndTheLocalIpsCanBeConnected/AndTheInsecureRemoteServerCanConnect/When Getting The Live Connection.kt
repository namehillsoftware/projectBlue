package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndAMediaCenterConnection.AndASecureServerIsFoundViaLookup.AndTheLocalIpsCanBeConnected.AndTheInsecureRemoteServerCanConnect

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class `When Getting The Live Connection` {

	private val services by lazy {

		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(777)) } returns Promise(
			ServerInfo(
				143,
				452,
				setOf("1.2.3.4"),
				setOf(
					"53.24.19.245",
					"192.168.1.56"
				), emptySet(),
				Hex.decodeHex("2386166660562C5AAA1253B2BED7C2483F9C2D45")
			)
		)

		LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			mockk {
				every { promiseConnectionSettings(LibraryId(777)) } returns MediaCenterConnectionSettings(
					accessCode = "gooPc"
				).toPromise()
			},
			mockk {
				every { promiseServerClient(any<MediaCenterConnectionDetails>()) } returns mockk<HttpPromiseClient> {
					every { promiseResponse(any()) } returns Promise(
						PassThroughHttpResponse(
							200,
							"Ok",
							"""
							<?xml version="1.0" encoding="UTF-8" standalone="yes" ?><Response Status="FAIL"></Response>
							""".trimIndent().toByteArray().inputStream()
						)
					)
				}.toPromise()

				every {
					promiseServerClient(match<MediaCenterConnectionDetails> { a ->
						listOf(
							"https://192.168.1.56:452",
							"http://1.2.3.4:143"
						).contains(a.baseUrl.toString())
					})
				} answers {
					val urlProvider = firstArg<MediaCenterConnectionDetails>()
					selectedConnectionDetails = urlProvider
					mockk<HttpPromiseClient> {
						every { promiseResponse(URL(urlProvider.baseUrl, "MCWS/v1/Alive")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"Ok",
								"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
									<Response Status="OK"></Response>""".toByteArray().inputStream()
							)
						)
					}.toPromise()
				}
			},
			mockk(),
			mockk(),
			mockk(),
            JsonEncoderDecoder,
			mockk(),
		)
	}

	private var selectedConnectionDetails: MediaCenterConnectionDetails? = null
	private var liveServerConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		liveServerConnection = services.promiseLiveServerConnection(LibraryId(777)).toExpiringFuture().get()
	}

	@Test
	fun `then the insecure url provider is returned`() {
		assertThat(liveServerConnection).isNotNull
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(selectedConnectionDetails?.baseUrl.toString()).isEqualTo("http://1.2.3.4:143")
	}
}
