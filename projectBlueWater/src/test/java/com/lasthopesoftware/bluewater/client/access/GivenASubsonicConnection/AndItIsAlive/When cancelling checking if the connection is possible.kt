package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndItIsAlive

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class `When cancelling checking if the connection is possible` {
	private val result by lazy {
		val response = PassThroughHttpResponse(
			200,
			"Hewwo",
			"""{"subsonic-response":{"status":"ok","version":"1.16.1","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true}}""".toByteArray().inputStream()
		)

		val deferredResponse = object : DeferredPromise<HttpResponse>(response) {
			override fun cancellationRequested() {
				reject(CancellationException("Cancelled!"))
			}
		}

		val connectionDetails = SubsonicConnectionDetails(TestUrl, "test", "OcKgeQpzgzl")
		val promisedTest = LiveSubsonicConnection(
			connectionDetails,
			mockk {
				every {
					promiseServerClient(connectionDetails)
				} answers {
					val sc = firstArg<SubsonicConnectionDetails>()
					mockk<HttpPromiseClient> {
						every { promiseResponse(sc.baseUrl.addPath("rest/ping.view").addParams("f=json")) } returns deferredResponse
					}.toPromise()
				}
			},
			mockk(),
            JsonEncoderDecoder,
			mockk(),
		).promiseIsConnectionPossible()
		promisedTest.cancel()
		deferredResponse.resolve()

		promisedTest.toExpiringFuture()[5, TimeUnit.SECONDS]
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isFalse
	}
}
