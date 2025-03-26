package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAPlaylistPath

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When saving a new playlist` {

	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "Playlists/Add", "Type=Playlist", "Path=My Fancy Album", "CreateMode=Overwrite")) {
				PassThroughHttpResponse(
					200,
					"OK",
					("""<Response Status="OK">
<Item Name="PlaylistID">554772758</Item>
</Response>""").toByteArray().inputStream()
				)
			}

			mapResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "Playlist/AddFiles", "PlaylistType=ID", "&Playlist=554772758", "Keys=954,172,366")) {
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
				mockk {
					every { getServerClient(any<MediaCenterConnectionDetails>()) } returns httpConnection
				},
				mockk(),
			)
		)
	}

	@BeforeAll
	fun act() {
		mut.second
			.promiseStoredPlaylist(
				"My Fancy Album",
				listOf(
					ServiceFile("954"),
					ServiceFile("172"),
					ServiceFile("366"),
				)
			)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the playlist is created correctly`() {
		assertThat(mut.first.recordedRequests).containsExactly(
			MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "Playlists/Add", "Type=Playlist", "Path=My Fancy Album", "CreateMode=Overwrite"),
			MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "Playlist/AddFiles", "PlaylistType=ID", "Playlist=554772758", "Keys=954,172,366")
		)
	}
}
