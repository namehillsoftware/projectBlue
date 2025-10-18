package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.random.Random.Default.nextInt

class WhenReceivingThePromisedProgramVersion {

	private val expectedVersion by lazy {
		SemanticVersion(nextInt(), nextInt(), nextInt())
	}

	private val version by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath("ping.view").addParams("f=json")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"""{"subsonic-response":{"status":"ok","version":"$expectedVersion","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true}}""".encodeToByteArray().inputStream()
				)
			}
		}

		val connection = LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "hi5ZMj3i", "7O1rsa2Yl"),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
            JsonEncoderDecoder,
			mockk(),
		)

		connection.promiseServerVersion().toExpiringFuture()[100, TimeUnit.MILLISECONDS]
	}

	@Test
	fun `then the server version is present`() {
		assertThat(version).isNotNull
	}

	@Test
	fun `then the server version is correct`() {
		assertThat(version).isEqualTo(expectedVersion)
	}
}
