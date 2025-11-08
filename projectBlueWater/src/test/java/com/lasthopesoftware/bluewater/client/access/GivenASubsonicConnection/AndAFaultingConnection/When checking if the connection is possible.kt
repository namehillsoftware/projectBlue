package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndAFaultingConnection

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

class `When checking if the connection is possible` {

	private val result by lazy {
		val connectionDetails = SubsonicConnectionDetails(TestUrl,"auth", "test", "xVXFSNI")
		LiveSubsonicConnection(
			connectionDetails,
			mockk {
				every {
					promiseServerClient(connectionDetails)
				} answers {
					val urlProvider = firstArg<SubsonicConnectionDetails>()
					mockk<HttpPromiseClient> {
						every { promiseResponse(urlProvider.baseUrl.withSubsonicApi().addPath("ping.view").addParams("f=json")) } returns Promise(IOException())
					}.toPromise()
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
