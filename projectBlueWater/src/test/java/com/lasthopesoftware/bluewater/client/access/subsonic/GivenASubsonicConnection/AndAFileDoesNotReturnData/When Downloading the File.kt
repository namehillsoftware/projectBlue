package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndAFileDoesNotReturnData

import android.os.Build
import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.emptyByteArray
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When Downloading the File` {
	private val inputStream by lazy {
		val downloader = LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "4fIgSH4dK", "jhLZT9ZK"),
			FakeHttpConnectionProvider(FakeHttpConnection().apply {
				mapResponse(
					MediaCenterUrlBuilder.buildUrl(
						TestMcwsUrl,
						"File/GetFile",
						"File=4",
						"Quality=Medium",
						"Conversion=Android",
						"Playback=0",
						"AndroidVersion=${Build.VERSION.RELEASE}")
				) {
					PassThroughHttpResponse(202, "Not found", emptyByteArray.inputStream())
				}
			}),
			mockk(),
		)
		downloader.promiseFile(ServiceFile("af6f5ae0b3b94ac4a5a8d9480b207bb7")).toExpiringFuture().get()
	}

	@Test
	fun `then an empty input stream is returned`() {
		assertThat(inputStream!!.available()).isEqualTo(0)
	}
}
