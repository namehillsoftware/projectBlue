package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndAPlaylistPath

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When updating a playlist` {

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
          "id": "3ad183c5-f87c-4632-a87e-cb784c694edd",
          "name": "Nuncvarius",
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
}""".trimIndent().toByteArray().inputStream()
				)
			}

			mapResponse(TestUrl.withSubsonicApi().addPath("createPlaylist").addParams( "playlistId=3ad183c5-f87c-4632-a87e-cb784c694edd", "songId=134" , "songId=64", "songId=43", "songId=61", "songId=760").addParams("f=json")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"""{"subsonic-response":{"status":"ok","version":"1.16.1","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true,"license":{"valid":true}}}""".toByteArray().inputStream()
				)
			}
		}

		Pair(
			httpConnection,
			LiveSubsonicConnection(
				SubsonicConnectionDetails(TestUrl, "sr4qDP3", "eSbzvtAsP4P"),
				mockk {
					every { promiseServerClient(any<SubsonicConnectionDetails>()) } returns httpConnection.toPromise()
				},
				mockk(),
                JsonEncoderDecoder,
				mockk(),
			)
		)
	}

	@BeforeAll
	fun act() {
		mut.second
			.promiseStoredPlaylist(
				"Nuncvarius",
				listOf(
					ServiceFile("134"),
					ServiceFile("64"),
					ServiceFile("43"),
					ServiceFile("61"),
					ServiceFile("760"),
				)
			)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the playlist is created correctly`() {
		assertThat(mut.first.recordedRequests).contains(
			TestUrl
				.withSubsonicApi()
				.addPath("createPlaylist")
				.addParams("f=json")
				.addParams( "playlistId=3ad183c5-f87c-4632-a87e-cb784c694edd", "songId=134" , "songId=64", "songId=43", "songId=61", "songId=760"),
		)
	}
}
