package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndAReadWriteLibraryServer

import com.lasthopesoftware.TestUrl
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

class `When Checking Authentication` {

	private val isReadOnly by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath("getUser").addParams("username=mh6qdj2kJH").addParams("f=json")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"""{
  "subsonic-response": {
    "status": "ok",
    "version": "1.16.1",
    "user": {
      "folder": [
          1,
          3
      ],
      "username": "HanRaniJi",
      "email": "RamonPang@Nullaclass.8a",
      "scrobblingEnabled": "true",
      "adminRole": "false",
      "settingsRole": "true",
      "downloadRole": "true",
      "uploadRole": "false",
      "playlistRole": "true",
      "coverArtRole": "true",
      "commentRole": "true",
      "podcastRole": "true",
      "streamRole": "true",
      "jukeboxRole": "true",
      "shareRole": "false"
    }
  }
}""".trimIndent().toByteArray().inputStream()
				)
			}
		}

		val access = LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "mh6qdj2kJH", "bNI1AuryTD"),
			FakeHttpConnectionProvider(httpConnection),
            JsonEncoderDecoder,
			mockk(),
		)
		access.promiseIsReadOnly().toExpiringFuture().get()
	}

	@Test
	fun `then the connection is not read only`() {
		assertThat(isReadOnly).isFalse
	}
}
