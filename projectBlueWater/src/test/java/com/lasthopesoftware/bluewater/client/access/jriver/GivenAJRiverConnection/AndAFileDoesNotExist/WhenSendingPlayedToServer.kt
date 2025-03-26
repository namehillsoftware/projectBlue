package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAFileDoesNotExist

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenSendingPlayedToServer {

	private val updater by lazy {
        LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
			mockk {
				every { getServerClient(any<MediaCenterConnectionDetails>()) } returns FakeHttpConnection()
			},
			mockk(),
		)
	}

	private var httpResponseException: HttpResponseException? = null

	@BeforeAll
	fun act() {
		try {
			updater.promisePlaystatsUpdate(ServiceFile("627")).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			httpResponseException = e.cause as? HttpResponseException
		}
	}

	@Test
	fun thenAnHttpResponseExceptionIsThrownWithTheResponseCode() {
		assertThat(httpResponseException!!.responseCode).isEqualTo(404)
	}
}
