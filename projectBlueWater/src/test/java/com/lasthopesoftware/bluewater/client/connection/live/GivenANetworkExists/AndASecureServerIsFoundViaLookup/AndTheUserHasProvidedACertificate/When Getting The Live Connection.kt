package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndASecureServerIsFoundViaLookup.AndTheUserHasProvidedACertificate

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
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

private const val libraryId = 973

class `When Getting The Live Connection` {

	private val serverConnection by lazy {
		val urlScanner = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			mockk {
				every { promiseServerInformation(LibraryId(libraryId)) } returns Promise(
					ServerInfo(
						717,
						617,
						"681.241.214.352",
						emptySet(),
						emptySet(),
						Hex.decodeHex("E5252D4CEFB873A93BA1D7017EFB47D09F8BA924")
					)
				)
			},
			mockk {
				every { lookupConnectionSettings(LibraryId(libraryId)) } returns MediaCenterConnectionSettings(
					accessCode = "gooPc",
					sslCertificateFingerprint = Hex.decodeHex("F951D0C4AC2778F5C36344D7F0CD6D61E4BFE01F")
				).toPromise()
			},
			mockk {
				every {
					getServerClient(match { a ->
						listOf(
							"https://681.241.214.352:617",
							"http://681.241.214.352:717"
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

		urlScanner.promiseLiveServerConnection(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(serverConnection?.serverConnection?.baseUrl.toString()).isEqualTo("https://681.241.214.352:617")
	}

	@Test
	fun `then the certificate fingerprint is correct`() {
		assertThat(serverConnection?.serverConnection?.certificateFingerprint)
			.isEqualTo(Hex.decodeHex("E5252D4CEFB873A93BA1D7017EFB47D09F8BA924"))
	}
}
