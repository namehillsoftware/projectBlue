package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection

import android.os.Build
import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.JRiverLibraryConnection
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.JRiverUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
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
		val downloader = JRiverLibraryConnection(
			ServerConnection(TestUrl),
			FakeHttpConnectionProvider(FakeHttpConnection().apply {
				mapResponse(JRiverUrlBuilder.getUrl(
					TestMcwsUrl,
					"File/GetFile",
					"File=4",
					"Quality=Medium",
					"Conversion=Android",
					"Playback=0",
					"AndroidVersion=${Build.VERSION.RELEASE}")
				) {
					PassThroughHttpResponse(
						200,
						"OK",
						responseBytes.inputStream()
					)
				}
			})
		)

		downloader.promiseFile(ServiceFile(4)).toExpiringFuture().get()
	}

	@Test
	fun `then the input stream is returned`() {
		val outputStream = ByteArrayOutputStream()
		IOUtils.copy(inputStream, outputStream)
		assertThat(outputStream.toByteArray()).containsExactly(*responseBytes)
	}
}
