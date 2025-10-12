package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAFileDoesNotReturnData

import android.os.Build
import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.emptyByteArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When Downloading the File` {
	private val inputStream by lazy {
		val downloader = LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
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
					PassThroughHttpResponse(202, "Not found", emptyByteArray.inputStream())
				}
			}),
        )
		downloader.promiseFile(ServiceFile("4")).toExpiringFuture().get()
	}

	@Test
	fun `then an empty input stream is returned`() {
		assertThat(inputStream!!.available()).isEqualTo(0)
	}
}
