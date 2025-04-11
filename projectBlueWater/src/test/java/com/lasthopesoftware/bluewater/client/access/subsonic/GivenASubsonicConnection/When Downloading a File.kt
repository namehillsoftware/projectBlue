package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection

import android.os.Build
import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.mockk
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.Random

class `When Downloading a File` {
	private val responseBytes by lazy {
		val bytes = ByteArray(400)
		Random().nextBytes(bytes)
		bytes
	}

	private val inputStream by lazy {
		val downloader = LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "9noKEJ0jr", "il1K4Mwo"),
			FakeHttpConnectionProvider(FakeHttpConnection().apply {
				mapResponse(
					TestMcwsUrl
						.addPath("File/GetFile")
						.addParams(
							"File=4",
							"Quality=Medium",
							"Conversion=Android",
							"Playback=0",
							"AndroidVersion=${Build.VERSION.RELEASE}"
						)
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
		)

		downloader.promiseFile(ServiceFile("69c8a22b41604844a698c2a6bb9635c2")).toExpiringFuture().get()
	}

	@Test
	fun `then the input stream is returned`() {
		val outputStream = ByteArrayOutputStream()
		IOUtils.copy(inputStream, outputStream)
		assertThat(outputStream.toByteArray()).containsExactly(*responseBytes)
	}
}
