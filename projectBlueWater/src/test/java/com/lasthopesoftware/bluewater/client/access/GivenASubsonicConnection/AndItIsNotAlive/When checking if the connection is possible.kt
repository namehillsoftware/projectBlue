package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndItIsNotAlive

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class `When checking if the connection is possible` {

	private val result by lazy {
		val connectionDetails = SubsonicConnectionDetails(TestUrl, "auth", "test", "80")
		LiveSubsonicConnection(
			connectionDetails,
			mockk {
				every {
					getServerClient(connectionDetails)
				} answers {
					val urlProvider = firstArg<SubsonicConnectionDetails>()
					mockk {
						every { promiseResponse(URL(urlProvider.baseUrl, "rest/ping.view").addParams("f=json")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"K",
								"""{"subsonic-response":{"status":"nok","version":"1.16.1","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true}}""".toByteArray().inputStream()
							)
						)
					}
				}
			},
			mockk(),
			JsonEncoderDecoder,
			mockk(),
		).promiseIsConnectionPossible().toExpiringFuture().get()!!
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isFalse
	}
}
