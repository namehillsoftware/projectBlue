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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting the file string list` {
	companion object {
		private const val itemId = "08969771808f4ef8af0ac227a78ea9af"
	}

	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath("getMusicDirectory").addParams("id=$itemId")) {
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
          "id": "a299e48d-3e90-40ff-9635-236da89bbab7",
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
          "id": "50e618a8-c53b-4aee-a7b5-3e4575365cfd",
          "parent": "d27e9ceac68a17257c85a4d433e7a4c8",
          "isDir": true,
          "title": "Acetiam Turpisac",
          "name": "Acetiam Turpisac",
          "album": "Acetiam Turpisac",
          "artist": "EvelynAslam",
          "genre": "40s",
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
          "id": "87e43a0e-f3d9-424b-a1c3-7bfd3ba4fb6c",
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
          "id": "20",
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
          "id": "aed2bcaf284946a98a2118c2e0f7a11f",
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
          "id": "e6246b0b-7fe2-4619-b73c-f85cc9db1231",
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
          "id": "cae2f598f0ae45828c8669706dbda06f",
          "parent": "290764db-833a-4ba4-bea2-b91444d26636",
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
			mockk(),
			JsonEncoderDecoder,
		)
	}

	private var stringList = ""

	@BeforeAll
	fun act() {
		stringList = mut.promiseFileStringList(ItemId(itemId)).toExpiringFuture().get()!!
	}

	@Test
	fun `then the items are correct`() {
		assertThat(stringList).isEqualTo("2;6;-1;a299e48d-3e90-40ff-9635-236da89bbab7;87e43a0e-f3d9-424b-a1c3-7bfd3ba4fb6c;20;aed2bcaf284946a98a2118c2e0f7a11f;e6246b0b-7fe2-4619-b73c-f85cc9db1231;cae2f598f0ae45828c8669706dbda06f;")
	}
}
