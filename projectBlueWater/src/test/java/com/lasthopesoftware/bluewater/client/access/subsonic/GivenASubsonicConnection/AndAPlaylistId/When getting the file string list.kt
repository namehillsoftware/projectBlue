package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndAPlaylistId

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
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

class `When getting the file string list` {
	companion object {
		private const val itemId = "08969771808f4ef8af0ac227a78ea9af"
	}

	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath("getPlaylist").addParams("id=$itemId")) {
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
    "playlist": {
      "id": "37ff086d-a35b-4888-9cd5-73d6db7b5e7f",
      "name": "Test",
      "songCount": 2,
      "duration": 226,
      "public": true,
      "owner": "navidrome",
      "created": "2025-02-10T04:26:50.999472737Z",
      "changed": "2025-02-10T04:27:23.88859635Z",
      "coverArt": "pl-37ff086d-a35b-4888-9cd5-73d6db7b5e7f_67a9802b",
      "entry": [
        {
          "id": "8aa9f91f0e4d31862b31c7fa3065ccaa"
        },
        {
          "id": "65b8a4f9f9f65054e3e914b556fe765d"
        },
        {
          "id": "69ee3f960a64494f87e1f0a1f22c2327"
        },
        {
          "id": "3d238294b2f9427080c61345a921d614"
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

	private var stringList = ""

	@BeforeAll
	fun act() {
		stringList = mut.promiseFileStringList(PlaylistId(itemId)).toExpiringFuture().get()!!
	}

	@Test
	fun `then the items are correct`() {
		assertThat(stringList).isEqualTo("2;4;-1;8aa9f91f0e4d31862b31c7fa3065ccaa;65b8a4f9f9f65054e3e914b556fe765d;69ee3f960a64494f87e1f0a1f22c2327;3d238294b2f9427080c61345a921d614;")
	}
}
