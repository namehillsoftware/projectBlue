package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAPlaylistPath

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When updating a playlist` {

	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestMcwsUrl.addPath("Playlists/Add").addParams("Type=Playlist", "Path=bold\\voyage", "CreateMode=Overwrite")) {
				PassThroughHttpResponse(
					200,
					"OK",
					("""<Response Status="OK">
<Item Name="PlaylistID">38981873</Item>
</Response>""").toByteArray().inputStream()
				)
			}

			mapResponse(TestMcwsUrl.addPath("Playlist/AddFiles").addParams("PlaylistType=ID", "Playlist=38981873", "Keys=885,481,139,935")) {
				PassThroughHttpResponse(
					200,
					"OK",
					("""<Response Status="OK" />""").toByteArray().inputStream()
				)
			}
		}

		Pair(
			httpConnection,
			LiveMediaCenterConnection(
				MediaCenterConnectionDetails(TestUrl),
				FakeHttpConnectionProvider(httpConnection),
				mockk(),
			)
        )
	}

	@BeforeAll
	fun act() {
		mut.second
			.promiseStoredPlaylist(
				"bold\\voyage",
				listOf(
                    ServiceFile("885"),
                    ServiceFile("481"),
                    ServiceFile("139"),
                    ServiceFile("935"),
				)
			)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the playlist is created correctly`() {
		assertThat(mut.first.recordedRequests).containsExactly(
			TestMcwsUrl.addPath("Playlists/Add").addParams("Type=Playlist", "Path=bold\\voyage", "CreateMode=Overwrite"),
			TestMcwsUrl.addPath("Playlist/AddFiles").addParams("PlaylistType=ID", "Playlist=38981873", "Keys=885,481,139,935"),
		)
	}
}
