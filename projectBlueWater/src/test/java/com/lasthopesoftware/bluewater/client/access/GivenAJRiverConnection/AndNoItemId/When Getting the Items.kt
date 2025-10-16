package com.lasthopesoftware.bluewater.client.access.GivenAJRiverConnection.AndNoItemId

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
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

class `When Getting the Items` {

	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestMcwsUrl.addPath( "Browse/Children").addParams("Version=2", "ErrorOnMissing=1")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"""
						<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
						<Response Status="OK">
						<Item Name="Muzac">269</Item>
						<Item Name="Plists">117</Item>
						</Response>
					""".encodeToByteArray().inputStream()
				)
			}
		}

		LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
        )
	}

	private var items = emptyList<IItem>()

	@BeforeAll
	fun act() {
		items = mut.promiseItems(null).toExpiringFuture().get()!!
	}

	@Test
	fun `then the items are correct`() {
		assertThat(items).containsExactly(
			Item(key="269", value="Muzac", playlistId=null),
			Item(key="117", value="Plists", playlistId=null),
		)
	}
}
