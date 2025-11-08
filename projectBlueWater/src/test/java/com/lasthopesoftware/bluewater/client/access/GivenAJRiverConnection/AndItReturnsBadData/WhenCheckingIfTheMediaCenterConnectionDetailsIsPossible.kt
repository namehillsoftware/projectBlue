package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItReturnsBadData

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class WhenCheckingIfTheMediaCenterConnectionDetailsIsPossible {

	private val result by lazy {
		val mediaCenterConnectionDetails = MediaCenterConnectionDetails("auth", "test", 80)
		LiveMediaCenterConnection(
			mediaCenterConnectionDetails,
			mockk {
				every {
					promiseServerClient(match<MediaCenterConnectionDetails> { a ->
						"http://test:80" == a.baseUrl.toString()
					})
				} answers {
					val urlProvider = firstArg<MediaCenterConnectionDetails>()
					mockk<HttpPromiseClient> {
						every { promiseResponse(URL(urlProvider.baseUrl, "MCWS/v1/Alive")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"K",
								("{Response Status=\"NOT-OK\"}}" +
									"<Item Name=\"Master\">1192</Item>" +
									"<Item Name=\"Sync\">1192</Item>" +
									"{{Item Name=\"LibraryStartup\">1501430846</Item>" +
									"</Response>"
									).toByteArray().inputStream()
							)
						)
					}.toPromise()
				}
			},
			mockk(),
        ).promiseIsConnectionPossible().toExpiringFuture().get()!!
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isFalse
	}
}
