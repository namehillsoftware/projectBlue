package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When loading the audio playlist paths` {
	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath("getPlaylists")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"""{"subsonic-response":{"status":"ok","version":"1.16.1","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true,"playlists":{"playlist":[{"id":"14f6b170-3eb1-4e17-995a-733cb23f5c9f","name":"Recently Played","comment":"Recently played tracks","songCount":1,"duration":147,"public":false,"owner":"navidrome","created":"2025-02-10T04:33:31.506347198Z","changed":"2025-03-31T00:21:34.112627553Z","coverArt":"pl-14f6b170-3eb1-4e17-995a-733cb23f5c9f_67a9824c"},{"id":"37ff086d-a35b-4888-9cd5-73d6db7b5e7f","name":"Test","songCount":2,"duration":226,"public":true,"owner":"navidrome","created":"2025-02-10T04:26:50.999472737Z","changed":"2025-02-10T04:27:23.88859635Z","coverArt":"pl-37ff086d-a35b-4888-9cd5-73d6db7b5e7f_67a9802b"}]}}}""".encodeToByteArray().inputStream()
				)
			}
		}

		LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "omN7D8FZvvD", "oHAJ8UOAo"),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)
	}

	private var audioPlaylist: List<String>? = null

	@BeforeAll
	fun act() {
		audioPlaylist = mut.promiseAudioPlaylistPaths().toExpiringFuture().get()
	}

	@Test
	fun `then the audio playlist is correct`() {
		assertThat(audioPlaylist).containsExactly(
			"Workout",
			"Nested\\4A"
		)
	}
}
