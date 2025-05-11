package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndAPlaylistId

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
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

class `When getting files` {

	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath("getPlaylist").addParams("id=37ff086d-a35b-4888-9cd5-73d6db7b5e7f").addParams("f=json")) {
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
          "id": "8aa9f91f0e4d31862b31c7fa3065ccaa",
          "parent": "c2696ddbcd8fc89c861547c307e0a076",
          "isDir": false,
          "title": "Can Things Be Better?",
          "album": "21 Grams",
          "artist": "Gustavo Santaolalla",
          "track": 2,
          "year": 2002,
          "genre": "Soundtrack",
          "coverArt": "mf-8aa9f91f0e4d31862b31c7fa3065ccaa_66d46a5c",
          "size": 1899566,
          "contentType": "audio/mpeg",
          "suffix": "mp3",
          "duration": 79,
          "bitRate": 189,
          "path": "Gustavo Santaolalla/21 Grams/02 - Can Things Be Better?.mp3",
          "created": "2025-02-10T04:25:54.893250427Z",
          "albumId": "c2696ddbcd8fc89c861547c307e0a076",
          "artistId": "38ec9eabef4778ac77923ad3a59a23f9",
          "type": "music",
          "isVideo": false,
          "bpm": 98,
          "comment": "",
          "sortName": "",
          "mediaType": "song",
          "musicBrainzId": "",
          "genres": [],
          "replayGain": {
            "trackGain": -6.07,
            "trackPeak": 0.891,
            "albumPeak": 1
          },
          "channelCount": 2,
          "samplingRate": 44100
        },
        {
          "id": "65b8a4f9f9f65054e3e914b556fe765d",
          "parent": "c2696ddbcd8fc89c861547c307e0a076",
          "isDir": false,
          "title": "Do We Lose 21 Grams?",
          "album": "21 Grams",
          "artist": "Gustavo Santaolalla",
          "track": 1,
          "year": 2002,
          "genre": "Soundtrack",
          "coverArt": "mf-65b8a4f9f9f65054e3e914b556fe765d_66d46a5c",
          "size": 3232942,
          "contentType": "audio/mpeg",
          "suffix": "mp3",
          "starred": "2025-02-10T04:35:03.601014507Z",
          "duration": 147,
          "bitRate": 173,
          "path": "Gustavo Santaolalla/21 Grams/01 - Do We Lose 21 Grams?.mp3",
          "playCount": 1,
          "created": "2025-02-10T04:25:54.893011148Z",
          "albumId": "c2696ddbcd8fc89c861547c307e0a076",
          "artistId": "38ec9eabef4778ac77923ad3a59a23f9",
          "type": "music",
          "userRating": 5,
          "isVideo": false,
          "played": "2025-02-10T04:27:05.511Z",
          "bpm": 105,
          "comment": "",
          "sortName": "",
          "mediaType": "song",
          "musicBrainzId": "",
          "genres": [],
          "replayGain": {
            "trackGain": -6.92,
            "trackPeak": 0.881,
            "albumPeak": 1
          },
          "channelCount": 2,
          "samplingRate": 44100
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

	private var items = emptyList<ServiceFile>()

	@BeforeAll
	fun act() {
		items = mut.promiseFiles(PlaylistId("37ff086d-a35b-4888-9cd5-73d6db7b5e7f")).toExpiringFuture().get()!!
	}

	@Test
	fun `then the items are correct`() {
		assertThat(items).containsExactly(
			ServiceFile("8aa9f91f0e4d31862b31c7fa3065ccaa"),
			ServiceFile("65b8a4f9f9f65054e3e914b556fe765d"),
		)
	}
}
