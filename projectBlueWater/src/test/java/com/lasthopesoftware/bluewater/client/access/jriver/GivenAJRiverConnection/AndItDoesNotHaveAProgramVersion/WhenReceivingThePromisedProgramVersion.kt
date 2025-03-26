package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItDoesNotHaveAProgramVersion

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenReceivingThePromisedProgramVersion {
    private val version by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "Alive")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"<Response Status=\"OK\"></Response>".toByteArray().inputStream()
				)
			}
		}

		val access = LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)
		access.promiseServerVersion().toExpiringFuture().get()
	}

    @Test
    fun `then the server version is null`() {
        assertThat(version).isNull()
    }
}
