package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndASecureServerIsFoundViaLookup.AndLocalOnlyIsSelected

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class `When Getting The Live Connection` {

	private val services by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(15)) } returns Promise(
			ServerInfo(
				143,
				45,
				setOf("1.2.3.4"),
				setOf(
					"53.24.19.245",
					"192.168.1.56"
				),
				emptySet(),
				ByteArray(0),
			)
		)

		LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			mockk {
				every { promiseConnectionSettings(LibraryId(15)) } returns MediaCenterConnectionSettings(
					accessCode = "gooPc",
					isLocalOnly = true
				).toPromise()
			},
			mockk {
				every { getServerClient(any<MediaCenterConnectionDetails>()) } returns mockk {
					every { promiseResponse(any()) } returns Promise(
						PassThroughHttpResponse(
							200,
							"Ok",
							"""
							<?xml version="1.0" encoding="UTF-8" standalone="yes" ?><Response Status="FAIL"></Response>
							""".trimIndent().toByteArray().inputStream()
						)
					)
				}

				every {
					getServerClient(match<MediaCenterConnectionDetails> { a ->
						listOf(
							"http://192.168.1.56:143",
							"https://192.168.1.56:143",
							"http://1.2.3.4:143"
						).contains(a.baseUrl.toString())
					})
				} answers {
					val urlProvider = firstArg<MediaCenterConnectionDetails>()
					selectedBaseUrl = urlProvider.baseUrl
					mockk {
						every { promiseResponse(URL(urlProvider.baseUrl, "MCWS/v1/Alive")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"Ok",
								"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
									<Response Status="OK"></Response>
										""".toByteArray().inputStream()
							)
						)
					}
				}
			},
			mockk(),
		)
	}

	private var selectedBaseUrl: URL? = null
	private var serverConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		serverConnection = services.promiseLiveServerConnection(LibraryId(15)).toExpiringFuture().get()
	}

	@Test
	fun `then the url provider is returned`() {
		assertThat(serverConnection).isNotNull
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(selectedBaseUrl?.toString()).isEqualTo("http://192.168.1.56:143")
	}
}
