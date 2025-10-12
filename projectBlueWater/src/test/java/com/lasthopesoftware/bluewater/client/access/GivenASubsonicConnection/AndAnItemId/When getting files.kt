package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndAnItemId

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
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

class `When getting files` {
	companion object {
		private const val itemId = "08969771808f4ef8af0ac227a78ea9af"
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
          "id": "300acf2d29fb0e3f7fd496a865f91b7b",
          "parent": "d2d9feeac6ffef402a44bc453b99da7c",
          "isDir": false,
          "title": "The Good Die Young",
          "album": "Power Of The Dollar",
          "artist": "50 Cent",
          "genre": "Gangsta Rap",
          "coverArt": "mf-300acf2d29fb0e3f7fd496a865f91b7b_6728cff2",
          "size": 5873499,
          "contentType": "audio/mpeg",
          "suffix": "mp3",
          "duration": 242,
          "bitRate": 192,
          "path": "50 Cent/Power Of The Dollar/The Good Die Young.mp3",
          "created": "2025-02-10T04:25:55.486432785Z",
          "albumId": "d2d9feeac6ffef402a44bc453b99da7c",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "type": "music",
          "isVideo": false,
          "bpm": 60,
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
            "trackGain": -3.35,
            "trackPeak": 1,
            "albumPeak": 1
          },
          "channelCount": 2,
          "samplingRate": 44100
        },
        {
          "id": "e4d231e0d63c5809d450eb8e97406062",
          "parent": "d2d9feeac6ffef402a44bc453b99da7c",
          "isDir": false,
          "title": "You Ain't No Gansta",
          "album": "Power Of The Dollar",
          "artist": "50 Cent",
          "genre": "Rap",
          "coverArt": "mf-e4d231e0d63c5809d450eb8e97406062_66d46a5c",
          "size": 5236568,
          "contentType": "audio/mpeg",
          "suffix": "mp3",
          "duration": 217,
          "bitRate": 192,
          "path": "50 Cent/Power Of The Dollar/You Ain't No Gansta.mp3",
          "created": "2025-02-10T04:25:55.53129563Z",
          "albumId": "d2d9feeac6ffef402a44bc453b99da7c",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "type": "music",
          "isVideo": false,
          "bpm": 96,
          "comment": "",
          "sortName": "",
          "mediaType": "song",
          "musicBrainzId": "",
          "genres": [
            {
              "name": "Rap"
            }
          ],
          "replayGain": {
            "trackGain": -5.86,
            "trackPeak": 1,
            "albumPeak": 1
          },
          "channelCount": 2,
          "samplingRate": 44100
        },
        {
          "id": "940c310c4110a023927b949af6cb01d3",
          "parent": "d2d9feeac6ffef402a44bc453b99da7c",
          "isDir": false,
          "title": "Your Life's On The Line",
          "album": "Power Of The Dollar",
          "artist": "50 Cent",
          "genre": "Rap",
          "coverArt": "mf-940c310c4110a023927b949af6cb01d3_66d46a5c",
          "size": 5313280,
          "contentType": "audio/mpeg",
          "suffix": "mp3",
          "duration": 220,
          "bitRate": 192,
          "path": "50 Cent/Power Of The Dollar/Your Life's On The Line.mp3",
          "created": "2025-02-10T04:25:55.533009769Z",
          "albumId": "d2d9feeac6ffef402a44bc453b99da7c",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "type": "music",
          "isVideo": false,
          "bpm": 46,
          "comment": "",
          "sortName": "",
          "mediaType": "song",
          "musicBrainzId": "",
          "genres": [
            {
              "name": "Rap"
            }
          ],
          "replayGain": {
            "trackGain": -7.69,
            "trackPeak": 0.977,
            "albumPeak": 1
          },
          "channelCount": 2,
          "samplingRate": 44100
        },
        {
          "id": "ed3b80a3371c05e96fbd68b00817b142",
          "parent": "d2d9feeac6ffef402a44bc453b99da7c",
          "isDir": false,
          "title": "Power of the Dollar",
          "album": "Power of The Dollar",
          "artist": "50 Cent",
          "track": 16,
          "genre": "Gangsta Rap",
          "coverArt": "mf-ed3b80a3371c05e96fbd68b00817b142_6728cff2",
          "size": 4991438,
          "contentType": "audio/mpeg",
          "suffix": "mp3",
          "duration": 206,
          "bitRate": 192,
          "path": "50 Cent/Power of The Dollar/16 - Power of the Dollar.mp3",
          "created": "2025-02-10T04:25:55.486992431Z",
          "albumId": "d2d9feeac6ffef402a44bc453b99da7c",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "type": "music",
          "isVideo": false,
          "bpm": 92,
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
            "trackGain": -5.64,
            "trackPeak": 1,
            "albumPeak": 1
          },
          "channelCount": 2,
          "samplingRate": 44100
        },
        {
          "id": "c157ee1df035f74a8aa40a3dfb72f3e8",
          "parent": "d2d9feeac6ffef402a44bc453b99da7c",
          "isDir": false,
          "title": "Power of the Dollar",
          "album": "Power of The Dollar",
          "artist": "50 Cent",
          "track": 16,
          "genre": "Rap",
          "coverArt": "mf-c157ee1df035f74a8aa40a3dfb72f3e8_66d46a5c",
          "size": 4972608,
          "contentType": "audio/mpeg",
          "suffix": "mp3",
          "duration": 206,
          "bitRate": 192,
          "path": "50 Cent/Power of The Dollar/16 - Power of the Dollar.mp3",
          "created": "2025-02-10T04:25:55.531945722Z",
          "albumId": "d2d9feeac6ffef402a44bc453b99da7c",
          "artistId": "d27e9ceac68a17257c85a4d433e7a4c8",
          "type": "music",
          "isVideo": false,
          "bpm": 92,
          "comment": "",
          "sortName": "",
          "mediaType": "song",
          "musicBrainzId": "",
          "genres": [
            {
              "name": "Rap"
            }
          ],
          "replayGain": {
            "trackGain": -5.64,
            "trackPeak": 1,
            "albumPeak": 1
          },
          "channelCount": 2,
          "samplingRate": 44100
        }
      ],
      "id": "d2d9feeac6ffef402a44bc453b99da7c",
      "name": "Power Of The Dollar",
      "parent": "d27e9ceac68a17257c85a4d433e7a4c8",
      "coverArt": "al-d2d9feeac6ffef402a44bc453b99da7c_6728cff2",
      "songCount": 32
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
            JsonEncoderDecoder,
			mockk(),
		)
	}

	private var items = emptyList<ServiceFile>()

	@BeforeAll
	fun act() {
		items = mut.promiseFiles(ItemId(itemId)).toExpiringFuture().get()!!
	}

	@Test
	fun `then the items are correct`() {
		assertThat(items).containsExactly(
			ServiceFile(key="03982302e52eaf593c7575dcc1d080ee"),
			ServiceFile(key="300acf2d29fb0e3f7fd496a865f91b7b"),
			ServiceFile(key="e4d231e0d63c5809d450eb8e97406062"),
			ServiceFile(key="940c310c4110a023927b949af6cb01d3"),
			ServiceFile(key="ed3b80a3371c05e96fbd68b00817b142"),
			ServiceFile(key="c157ee1df035f74a8aa40a3dfb72f3e8")
		)
	}
}
