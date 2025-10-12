package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndItReturnsARandomErrorCode

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Random
import java.util.concurrent.ExecutionException

class WhenSendingPlayedToServer {
	private val expectedResponseCode by lazy {
		val random = Random()
		random.nextInt(300, 600)
	}

	private val updater by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath( "scrobble").addParams("id=7236aa0f123443348389a0bb6573567b").addParams("f=json")) {
				PassThroughHttpResponse(
					expectedResponseCode,
					"NOK",
					emptyByteArray.inputStream()
				)
			}
		}

		LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "48CMGdBD6JS", "KkIapoI"),
			FakeHttpConnectionProvider(httpConnection),
            JsonEncoderDecoder,
			mockk(),
		)
	}
	private var httpResponseException: HttpResponseException? = null

	@BeforeAll
	fun act() {
		try {
			updater.promisePlaystatsUpdate(ServiceFile("7236aa0f123443348389a0bb6573567b")).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			httpResponseException = e.cause as? HttpResponseException
		}
	}

	@Test
	fun thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException!!.responseCode).isEqualTo(expectedResponseCode)
	}
}
