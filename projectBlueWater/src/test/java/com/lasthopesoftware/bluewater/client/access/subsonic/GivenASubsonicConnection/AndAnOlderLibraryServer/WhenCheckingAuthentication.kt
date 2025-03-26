package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndAnOlderLibraryServer

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenCheckingAuthentication {

	private val isReadOnly by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "Authenticate")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"""{"subsonic-response":{"status":"ok","version":"1.16.1","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true,"license":{"valid":true}}}""".toByteArray().inputStream()
				)
			}
		}

		val access = LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "zSaHtBXlict", "KtzJVjBK"),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)
		access.promiseIsReadOnly().toExpiringFuture().get()
	}

	@Test
	fun `then the connection is not read only`() {
		assertThat(isReadOnly).isFalse
	}
}
