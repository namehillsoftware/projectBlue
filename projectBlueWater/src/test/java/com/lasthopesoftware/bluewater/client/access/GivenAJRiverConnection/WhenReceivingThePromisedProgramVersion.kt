package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt

class WhenReceivingThePromisedProgramVersion {

	private val expectedVersion by lazy {
		SemanticVersion(nextInt(), nextInt(), nextInt())
	}

	private val version by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestMcwsUrl.addPath("Alive")) {
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

		val connection = LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
        )

		connection.promiseServerVersion().toExpiringFuture().get()
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
