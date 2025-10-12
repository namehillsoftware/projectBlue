package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
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

class `When getting an item image` {
	companion object {
		private const val itemId = "ffe17b192f724d0cbd36c6b7962a6d35"
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
						.addParams("id=$itemId")
						.addParams("f=json")
				) {
					PassThroughHttpResponse(
						200,
						"OK",
						responseBytes.inputStream()
					)
				}
			}),
            JsonEncoderDecoder,
			mockk(),
		)

		downloader.promiseImageBytes(ItemId(itemId)).toExpiringFuture().get()
	}

	@Test
	fun `then the image bytes are correct`() {
		assertThat(imageBytes).containsExactly(*responseBytes)
	}
}
