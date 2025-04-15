package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndASubsonicConnection.AndAServerIsFoundViaLookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
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
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(55)) } returns Promise(
			ServerInfo(
				17,
				null,
				setOf("3wV3e1Q"),
				emptySet(),
				emptySet(),
				emptyByteArray
			)
		)

		val urlScanner = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			mockk {
				every { promiseConnectionSettings(LibraryId(55)) } returns SubsonicConnectionSettings(
					url = "3wV3e1Q",
					userName = "fyOXmq0UHH",
					password = "jhpmi6SUf",
				).toPromise()
			},
			mockk {
				every {
					getServerClient(match<SubsonicConnectionDetails> { a ->
						listOf("http://3wV3e1Q:17").contains(a.baseUrl.toString())
					})
				} answers {
					val urlProvider = firstArg<SubsonicConnectionDetails>()
					selectedConnectionDetails = urlProvider
					mockk {
						every { promiseResponse(URL(urlProvider.baseUrl, "rest/ping.view")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"Ok",
								"""{"subsonic-response":{"status":"ok","version":"1.16.1","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true}}""".toByteArray().inputStream()
							)
						)
					}
				}
			},
			mockk(),
			JsonEncoderDecoder,
			mockk(),
		)

		urlScanner
	}

	private var selectedConnectionDetails: SubsonicConnectionDetails? = null
	private var serverConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		serverConnection = mutt.promiseLiveServerConnection(LibraryId(55)).toExpiringFuture().get()
	}

	@Test
	fun `then the server connection is returned`() {
		assertThat(serverConnection).isNotNull
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(selectedConnectionDetails?.baseUrl.toString()).isEqualTo("http://3wV3e1Q:17")
	}

	@Test
	fun `then the salt is not empty`() {
		assertThat(selectedConnectionDetails?.salt).isNotEmpty
	}
}
