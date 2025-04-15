package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndAFileDoesNotReturnData

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When Downloading the File` {
	private val inputStream by lazy {
		val downloader = LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "4fIgSH4dK", "jhLZT9ZK"),
			FakeHttpConnectionProvider(FakeHttpConnection().apply {
				mapResponse(
					TestUrl
						.withSubsonicApi()
						.addPath("stream")
						.addParams(
							"id=af6f5ae0b3b94ac4a5a8d9480b207bb7",
							"format=mp3",
							"maxBitRate=128",
						)
				) {
					PassThroughHttpResponse(202, "Not found", emptyByteArray.inputStream())
				}
			}),
			mockk(),
			JsonEncoderDecoder,
			mockk(),
		)
		downloader.promiseFile(ServiceFile("af6f5ae0b3b94ac4a5a8d9480b207bb7")).toExpiringFuture().get()
	}

	@Test
	fun `then an empty input stream is returned`() {
		assertThat(inputStream!!.available()).isEqualTo(0)
	}
}
