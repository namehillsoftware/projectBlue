package com.lasthopesoftware.bluewater.client.access.GivenAJRiverConnection.AndAPlaylistId

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withMcApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting files` {

	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withMcApi().addPath("Playlist/Files").addParams("Playlist=-466327", "Action=Serialize")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"2;20;-1;6830841;5772590;5667732;6385489;7477524;3685;937552;6271374;1167729;5204876;6515467;2338;6227150;937460;5691040;5729177;6271773;6241105;6628853;6082656".encodeToByteArray().inputStream()
				)
			}
		}

		LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)
	}

	private var items = emptyList<ServiceFile>()

	@BeforeAll
	fun act() {
		items = mut.promiseFiles(PlaylistId("-466327")).toExpiringFuture().get()!!
	}

	@Test
	fun `then the items are correct`() {
		assertThat(items).containsExactly(
			ServiceFile(key="6830841"),
			ServiceFile(key="5772590"),
			ServiceFile(key="5667732"),
			ServiceFile(key="6385489"),
			ServiceFile(key="7477524"),
			ServiceFile(key="3685"),
			ServiceFile(key="937552"),
			ServiceFile(key="6271374"),
			ServiceFile(key="1167729"),
			ServiceFile(key="5204876"),
			ServiceFile(key="6515467"),
			ServiceFile(key="2338"),
			ServiceFile(key="6227150"),
			ServiceFile(key="937460"),
			ServiceFile(key="5691040"),
			ServiceFile(key="5729177"),
			ServiceFile(key="6271773"),
			ServiceFile(key="6241105"),
			ServiceFile(key="6628853"),
			ServiceFile(key="6082656"),
		)
	}
}
