package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItReturnsARandomErrorCode

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.MediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
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
			mapResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "File/Played", "File=15", "FileType=Key")) {
				PassThroughHttpResponse(
					expectedResponseCode,
					"NOK",
					emptyByteArray.inputStream()
				)
			}
		}

		MediaCenterConnection(
			ServerConnection(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)
	}
	private var httpResponseException: HttpResponseException? = null

	@BeforeAll
	fun act() {
		try {
			updater.promisePlaystatsUpdate(ServiceFile(15)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			httpResponseException = e.cause as? HttpResponseException
		}
	}

	@Test
	fun thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException!!.responseCode).isEqualTo(expectedResponseCode)
	}
}
