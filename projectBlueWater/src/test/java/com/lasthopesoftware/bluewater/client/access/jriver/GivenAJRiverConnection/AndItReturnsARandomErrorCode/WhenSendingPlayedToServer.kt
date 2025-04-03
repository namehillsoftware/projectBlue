package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItReturnsARandomErrorCode

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.emptyByteArray
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
			mapResponse(TestMcwsUrl.addPath("File/Played").addParams("File=15", "FileType=Key")) {
				PassThroughHttpResponse(
					expectedResponseCode,
					"NOK",
					emptyByteArray.inputStream()
				)
			}
		}

		LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)
	}
	private var httpResponseException: HttpResponseException? = null

	@BeforeAll
	fun act() {
		try {
			updater.promisePlaystatsUpdate(ServiceFile("15")).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			httpResponseException = e.cause as? HttpResponseException
		}
	}

	@Test
	fun thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException!!.responseCode).isEqualTo(expectedResponseCode)
	}
}
