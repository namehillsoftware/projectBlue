package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndItReturnsBadData

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class `When checking if the connection is possible` {

	private val result by lazy {
		val connectionDetails = SubsonicConnectionDetails(TestUrl,"wifZkdUcp", "1pacEhQuA")
		LiveSubsonicConnection(
			connectionDetails,
			mockk {
				every {
					getServerClient(match<SubsonicConnectionDetails> { a -> TestUrl == a.baseUrl })
				} answers {
					val urlProvider = firstArg<SubsonicConnectionDetails>()
					mockk {
						every { promiseResponse(urlProvider.baseUrl.withSubsonicApi().addPath("ping.view")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"K",
								("{Response Status=\"NOT-OK\"}}" +
									"<Item Name=\"Master\">2</Item>" +
									"<Item Name=\"Sync\">171</Item>" +
									"{{Item Name=\"LibraryStartup\">1501430846</Item>" +
									"</Response>"
									).toByteArray().inputStream()
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
