package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndAMediaCenterConnection.AndAServerIsFoundViaLookup.AndAnAuthKeyIsProvided

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class WhenScanningForUrls {

	private val mutt by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(15)) } returns Promise(
			ServerInfo(
				143,
				null,
				setOf("1.2.3.4"), emptySet(), emptySet(),
				ByteArray(0)
			)
		)

		val urlScanner = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			mockk { every { encodeString(any()) } returns "gooey" },
			serverLookup,
			mockk {
				every { promiseConnectionSettings(LibraryId(15)) } returns MediaCenterConnectionSettings(
					accessCode = "gooPc",
					userName = "myuser",
					password = "myPass"
				).toPromise()
			},
			mockk {
				every {
					promiseServerClient(match<MediaCenterConnectionDetails> { a ->
						listOf("http://1.2.3.4:143").contains(a.baseUrl.toString()) && a.authCode == "gooey"
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

		urlScanner
	}

	private var selectedConnectionDetails: MediaCenterConnectionDetails? = null
	private var serverConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		serverConnection = mutt.promiseLiveServerConnection(LibraryId(15)).toExpiringFuture().get()
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
