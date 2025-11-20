package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndAMediaCenterConnection.AndAServerIsFoundViaLookup.AndTheLocalIpsCanBeConnected

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
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class `When Getting The Live Connection` {

	private val mutt by lazy {
		val serverLookup = mockk<LookupServers>() {
			every { promiseServerInformation(LibraryId(17)) } returns Promise(
				ServerInfo(
					143,
					null,
					setOf("1.2.3.4"),
					setOf(
						"53.24.19.245",
						"192.168.1.56"
					), emptySet(),
					emptyByteArray
				)
			)
		}

		val urlScanner = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			mockk {
				every { promiseConnectionSettings(LibraryId(17)) } returns MediaCenterConnectionSettings(accessCode = "gooPc").toPromise()
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
						listOf("http://192.168.1.56:143").contains(a.baseUrl.toString())
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

						every { promiseResponse(URL(urlProvider.baseUrl, "MCWS/v1/Authenticate")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"K",
								"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
										<Response Status="OK">
										<Item Name="Token">45tpH5JP1f</Item>
										<Item Name="ReadOnly">0</Item>
										<Item Name="PreLicensed">0</Item>
										</Response>
										""".toByteArray().inputStream()
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

		urlScanner
	}

	private var selectedConnectionDetails: MediaCenterConnectionDetails? = null
	private var serverConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		serverConnection = mutt.promiseLiveServerConnection(LibraryId(17)).toExpiringFuture().get()
	}

	@Test
	fun `then the server connection is returned`() {
		assertThat(serverConnection).isNotNull
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(selectedConnectionDetails?.baseUrl.toString()).isEqualTo("http://192.168.1.56:143")
	}
}
