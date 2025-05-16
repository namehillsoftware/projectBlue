package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndASubsonicConnection.AndASecureServerIsFoundViaLookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
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

class `When Getting The Live Connection` {

	private val services by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(35)) } returns Promise(
			ServerInfo(
				null,
				118,
				setOf("dR7ft3qmY99"),
				emptySet(),
				emptySet(),
				Hex.decodeHex("ae16487d40f24abb8c61f49bf4540076")
			)
		)

		val urlScanner = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			mockk {
				every { promiseConnectionSettings(LibraryId(35)) } returns SubsonicConnectionSettings(
					url = "dR7ft3qmY99",
					userName = "UT4tdcIOw3z",
					password = "9EIZHyoPY",
				).toPromise()
			},
			mockk {
				every {
					getServerClient(match<SubsonicConnectionDetails> { a ->
						a.baseUrl.toString() == "https://dR7ft3qmY99:118"
					})
				} answers {
					val urlProvider = firstArg<SubsonicConnectionDetails>()
					selectedConnectionDetails = urlProvider
					mockk {
						every { promiseResponse(urlProvider.baseUrl.addPath("rest/ping.view").addParams("f=json")) } returns Promise(
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
		serverConnection = services.promiseLiveServerConnection(LibraryId(35)).toExpiringFuture().get()
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(selectedConnectionDetails?.baseUrl.toString()).isEqualTo("https://dR7ft3qmY99:118")
	}

	@Test
	fun `then the certificate fingerprint is correct`() {
		assertThat(selectedConnectionDetails?.certificateFingerprint)
			.isEqualTo(Hex.decodeHex("ae16487d40f24abb8c61f49bf4540076"))
	}

	@Test
	fun `then the username is correct`() {
		assertThat(selectedConnectionDetails?.userName).isEqualTo("UT4tdcIOw3z")
	}

	@Test
	fun `then the password is correct`() {
		assertThat(selectedConnectionDetails?.password).isEqualTo("9EIZHyoPY")
	}
}
