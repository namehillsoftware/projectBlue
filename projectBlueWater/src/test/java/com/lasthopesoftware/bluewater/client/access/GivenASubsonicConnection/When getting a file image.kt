package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection

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
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Random

class `When getting a file image` {
	companion object {
		private const val serviceFileId = "cde0c6f16ecb443784c0e6dcd15b97de"
	}

	private val responseBytes by lazy {
		val bytes = ByteArray(200)
		Random().nextBytes(bytes)
		bytes
	}

	private val imageBytes by lazy {
		val downloader = LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "9noKEJ0jr", "il1K4Mwo"),
			FakeHttpConnectionProvider(FakeHttpConnection().apply {
				mapResponse(
					TestUrl
						.withSubsonicApi()
						.addPath("getCoverArt")
						.addParams("id=$serviceFileId")
						.addParams("f=json")
				) {
					PassThroughHttpResponse(
						200,
						"OK",
						responseBytes.inputStream()
					)
				}
			}),
			mockk(),
            JsonEncoderDecoder,
			mockk(),
		)

		downloader.promiseImageBytes(ServiceFile(serviceFileId)).toExpiringFuture().get()
	}

	@Test
	fun `then the image bytes are correct`() {
		assertThat(imageBytes).containsExactly(*responseBytes)
	}
}
