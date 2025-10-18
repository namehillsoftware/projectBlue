package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting items` {

	companion object {
		private const val itemId = "0266a31e78a74c048b05193470620d2e"
	}

	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath("getMusicDirectory").addParams("id=$itemId").addParams("f=json")) {
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
    "directory": {
      "child": [
        {
          "id": "d2d9feeac6ffef402a44bc453b99da7c",
          "parent": "d27e9ceac68a17257c85a4d433e7a4c8",
          "isDir": true,
          "title": "Power Of The Dollar",
          "name": "Power Of The Dollar",
          "album": "Power Of The Dollar",
          "artist": "50 Cent",
          "genre": "Gangsta Rap",
          "coverArt": "al-d2d9feeac6ffef402a44bc453b99da7c_6728cff2",
          "duration": 6613,
          "created": "2025-02-10T04:25:55.484218575Z",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "songCount": 32,
          "isVideo": false,
          "bpm": 0,
          "comment": "",
          "sortName": "",
          "mediaType": "album",
          "musicBrainzId": "",
          "genres": [],
          "replayGain": {},
          "channelCount": 0,
          "samplingRate": 0
        },
        {
          "id": "03982302e52eaf593c7575dcc1d080ee",
          "parent": "d2d9feeac6ffef402a44bc453b99da7c",
          "isDir": false,
          "title": "Money By Any Means",
          "album": "Power Of The Dollar",
          "artist": "50 Cent",
          "genre": "Gangsta Rap",
          "coverArt": "mf-03982302e52eaf593c7575dcc1d080ee_6728cff2",
          "size": 5899375,
          "contentType": "audio/mpeg",
          "suffix": "mp3",
          "duration": 243,
          "bitRate": 192,
          "path": "50 Cent/Power Of The Dollar/Money By Any Means.mp3",
          "created": "2025-02-10T04:25:55.486129739Z",
          "albumId": "d2d9feeac6ffef402a44bc453b99da7c",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "type": "music",
          "isVideo": false,
          "bpm": 61,
          "comment": "",
          "sortName": "",
          "mediaType": "song",
          "musicBrainzId": "",
          "genres": [
            {
              "name": "Gangsta Rap"
            }
          ],
          "replayGain": {
            "trackGain": -3.88,
            "trackPeak": 1,
            "albumPeak": 1
          },
          "channelCount": 2,
          "samplingRate": 44100
        },
        {
          "id": "e83d9ef8c135def872345aea30106d0f",
          "parent": "d27e9ceac68a17257c85a4d433e7a4c8",
          "isDir": true,
          "title": "Get Rich or Die Tryin'",
          "name": "Get Rich or Die Tryin'",
          "album": "Get Rich or Die Tryin'",
          "artist": "50 Cent",
          "year": 2003,
          "genre": "Rap",
          "coverArt": "al-e83d9ef8c135def872345aea30106d0f_66d46a5c",
          "duration": 4178,
          "created": "2025-02-10T04:25:55.408988653Z",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "songCount": 19,
          "isVideo": false,
          "bpm": 0,
          "comment": "",
          "sortName": "",
          "mediaType": "album",
          "musicBrainzId": "",
          "genres": [],
          "replayGain": {},
          "channelCount": 0,
          "samplingRate": 0
        },
        {
          "id": "9d266501d22a0c84472ecaa23b8db73f",
          "parent": "d27e9ceac68a17257c85a4d433e7a4c8",
          "isDir": true,
          "title": "Get Rich or Die Tryin'",
          "name": "Get Rich or Die Tryin'",
          "album": "Get Rich or Die Tryin'",
          "artist": "50 Cent",
          "year": 2003,
          "genre": "Rap",
          "coverArt": "al-9d266501d22a0c84472ecaa23b8db73f_6728cfeb",
          "duration": 4178,
          "created": "2025-02-10T04:25:55.33441888Z",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "songCount": 19,
          "isVideo": false,
          "bpm": 0,
          "comment": "",
          "sortName": "",
          "mediaType": "album",
          "musicBrainzId": "ba7f2a4e-eaf2-4366-be8c-41b2ef37b38e",
          "genres": [],
          "replayGain": {},
          "channelCount": 0,
          "samplingRate": 0
        },
        {
          "id": "0db42f9e040aeeae34bd35ce63464209",
          "parent": "d27e9ceac68a17257c85a4d433e7a4c8",
          "isDir": true,
          "title": "The Massacre",
          "name": "The Massacre",
          "album": "The Massacre",
          "artist": "50 Cent",
          "year": 2005,
          "genre": "Rap",
          "coverArt": "al-0db42f9e040aeeae34bd35ce63464209_6728cff6",
          "duration": 6240,
          "created": "2025-02-10T04:25:55.633891786Z",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "songCount": 30,
          "isVideo": false,
          "bpm": 0,
          "comment": "",
          "sortName": "",
          "mediaType": "album",
          "musicBrainzId": "",
          "genres": [],
          "replayGain": {},
          "channelCount": 0,
          "samplingRate": 0
        },
        {
          "id": "0b8e618220fa294132152bf81d6071bf",
          "parent": "d27e9ceac68a17257c85a4d433e7a4c8",
          "isDir": true,
          "title": "5 (Murder By Numbers)",
          "name": "5 (Murder By Numbers)",
          "album": "5 (Murder By Numbers)",
          "artist": "50 Cent",
          "year": 2012,
          "genre": "Rap",
          "coverArt": "al-0b8e618220fa294132152bf81d6071bf_66d46a5c",
          "duration": 1887,
          "created": "2025-02-10T04:25:55.294666605Z",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "songCount": 10,
          "isVideo": false,
          "bpm": 0,
          "comment": "",
          "sortName": "",
          "mediaType": "album",
          "musicBrainzId": "",
          "genres": [],
          "replayGain": {},
          "channelCount": 0,
          "samplingRate": 0
        },
        {
          "id": "eac5478331ec873e145815b76b934342",
          "parent": "d27e9ceac68a17257c85a4d433e7a4c8",
          "isDir": true,
          "title": "5 (Murder by Numbers)",
          "name": "5 (Murder by Numbers)",
          "album": "5 (Murder by Numbers)",
          "artist": "50 Cent",
          "year": 2012,
          "genre": "Rap",
          "coverArt": "al-eac5478331ec873e145815b76b934342_6728cfe9",
          "duration": 1887,
          "created": "2025-02-10T04:25:55.233578279Z",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "songCount": 10,
          "isVideo": false,
          "bpm": 0,
          "comment": "",
          "sortName": "",
          "mediaType": "album",
          "musicBrainzId": "7f3fd3ca-c0b0-4968-a418-3cf02d4b3943",
          "genres": [],
          "replayGain": {},
          "channelCount": 0,
          "samplingRate": 0
        }
      ],
      "id": "d27e9ceac68a17257c85a4d433e7a4c8",
      "name": "50 Cent",
      "albumCount": 6
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

	private var items = emptyList<IItem>()

	@BeforeAll
	fun act() {
		items = mut.promiseItems(ItemId(itemId)).toExpiringFuture().get()!!
	}

	@Test
	fun `then the items are correct`() {
		assertThat(items).containsExactly(
			Item(key="d2d9feeac6ffef402a44bc453b99da7c", value="Power Of The Dollar", playlistId=null),
			Item(key="e83d9ef8c135def872345aea30106d0f", value="Get Rich or Die Tryin'", playlistId=null),
			Item(key="9d266501d22a0c84472ecaa23b8db73f", value="Get Rich or Die Tryin'", playlistId=null),
			Item(key="0db42f9e040aeeae34bd35ce63464209", value="The Massacre", playlistId=null),
			Item(key="0b8e618220fa294132152bf81d6071bf", value="5 (Murder By Numbers)", playlistId=null),
			Item(key="eac5478331ec873e145815b76b934342", value="5 (Murder by Numbers)", playlistId=null),
		)
	}
}
