package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
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
			mapResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "Alive")) {
				PassThroughHttpResponse(
					200,
					"OK",
					("<Response Status=\"OK\">" +
						"<Item Name=\"RuntimeGUID\">{7FF5918E-9FDE-4D4D-9AE7-62DFFDD64397}</Item>" +
						"<Item Name=\"LibraryVersion\">24</Item><Item Name=\"ProgramName\">JRiver Media Center</Item>" +
						"<Item Name=\"ProgramVersion\">$expectedVersion</Item>" +
						"<Item Name=\"FriendlyName\">Media-Pc</Item>" +
						"<Item Name=\"AccessKey\">nIpfQr</Item>" +
						"</Response>").encodeToByteArray().inputStream()
				)
			}
		}

		val connection = MediaCenterConnection(
			ServerConnection(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
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
