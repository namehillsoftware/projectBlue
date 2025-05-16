package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection

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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When loading the audio playlist paths` {
	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath("getPlaylists").addParams("f=json")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"""
{
  "subsonic-response": {
    "status": "ok",
    "version": "1.16.1",
    "type": "navidrome",
    "serverVersion": "0.53.3 (13af8ed4)",
    "openSubsonic": true,
    "playlists": {
      "playlist": [
        {
          "id": "14f6b170-3eb1-4e17-995a-733cb23f5c9f",
          "name": "cSw5IEeK1k",
          "comment": "Recently played tracks",
          "songCount": 1,
          "duration": 147,
          "public": false,
          "owner": "navidrome",
          "created": "2025-02-10T04:33:31.506347198Z",
          "changed": "2025-04-15T02:27:28.667682041Z",
          "coverArt": "pl-14f6b170-3eb1-4e17-995a-733cb23f5c9f_67a9824c"
        },
        {
          "id": "37ff086d-a35b-4888-9cd5-73d6db7b5e7f",
          "name": "VDEQ2gkeoUV",
          "songCount": 2,
          "duration": 226,
          "public": true,
          "owner": "navidrome",
          "created": "2025-02-10T04:26:50.999472737Z",
          "changed": "2025-02-10T04:27:23.88859635Z",
          "coverArt": "pl-37ff086d-a35b-4888-9cd5-73d6db7b5e7f_67a9802b"
        },
        {
          "id": "adaf134b-a437-47be-9e3c-da6724c93964",
          "name": "Tt3fME4fr",
          "songCount": 2,
          "duration": 226,
          "public": true,
          "owner": "navidrome",
          "created": "2025-02-10T04:26:50.999472737Z",
          "changed": "2025-02-10T04:27:23.88859635Z",
          "coverArt": "pl-37ff086d-a35b-4888-9cd5-73d6db7b5e7f_67a9802b"
        }
      ]
    }
  }
}
					""".encodeToByteArray().inputStream()
				)
			}
		}

		LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "Xtbyp9lhSJY", "WVe5KSj"),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
			JsonEncoderDecoder,
			mockk(),
		)
	}

	private var playlistPaths = emptyList<String>()

	@BeforeAll
	fun act() {
		playlistPaths = mut.promiseAudioPlaylistPaths().toExpiringFuture().get()!!
	}

	@Test
	fun `then the items are correct`() {
		assertThat(playlistPaths).containsExactly(
			"cSw5IEeK1k",
			"VDEQ2gkeoUV",
			"Tt3fME4fr",
		)
	}
}
