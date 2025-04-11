package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndAMediaCenterConnection.AndAServerIsFoundViaLookup.AndTheUserNameIsAnEmptyString

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

	private val services by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(44)) } returns Promise(
			ServerInfo(
				143,
				null,
				setOf("1.2.3.4"), emptySet(), emptySet(),
				emptyByteArray
			)
		)

		val connectionProvider = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			mockk {
				every { promiseConnectionSettings(LibraryId(44)) } returns MediaCenterConnectionSettings(
					accessCode = "gooPc",
					userName = "",
					password = ""
				).toPromise()
			},
			mockk {
				every {
					getServerClient(match<MediaCenterConnectionDetails> { a ->
						listOf("http://1.2.3.4:143").contains(a.baseUrl.toString()) && a.authCode == null
					})
				} answers {
					val urlProvider = firstArg<MediaCenterConnectionDetails>()
					selectedConnectionDetails = urlProvider
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
			JsonEncoderDecoder,
		)

		connectionProvider
	}

	private var selectedConnectionDetails: MediaCenterConnectionDetails? = null
	private var serverConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		serverConnection = services.promiseLiveServerConnection(LibraryId(44)).toExpiringFuture().get()
	}

	@Test
	fun `then the server connection is returned`() {
		assertThat(serverConnection).isNotNull
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(selectedConnectionDetails?.baseUrl.toString()).isEqualTo("http://1.2.3.4:143")
	}
}
