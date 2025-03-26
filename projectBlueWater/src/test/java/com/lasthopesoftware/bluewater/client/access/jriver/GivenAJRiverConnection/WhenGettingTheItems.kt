package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenGettingTheItems {

	companion object {
		private const val itemId = "398"
	}

	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "Browse/Children", "ID=$itemId", "Version=2", "ErrorOnMissing=1")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"""
						<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
						<Response Status="OK">
						<Item Name="A.A. Bondy">1693</Item>
						<Item Name="Billy Bragg &amp; Wilco">1769</Item>
						<Item Name="BØRNS">1790</Item>
						<Item Name="Bruce Springsteen">1801</Item>
						<Item Name="Fleetwood Mac">1906</Item>
						<Item Name="Harvey Danger">1961</Item>
						<Item Name="The Head And The Heart">1962</Item>
						<Item Name="Hozier">1972</Item>
						<Item Name="Hurray For The Riff Raff">1973</Item>
						<Item Name="Shakey Graves">2221</Item>
						<Item Name="The Shins">2222</Item>
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

	private var items = emptyList<Item>()

	@BeforeAll
	fun act() {
		items = mut.promiseItems(ItemId(itemId)).toExpiringFuture().get()!!
	}

	@Test
	fun `then the items are correct`() {
		assertThat(items).containsExactly(
			Item(key="1693", value="A.A. Bondy", playlistId=null),
			Item(key="1769", value="Billy Bragg & Wilco", playlistId=null),
			Item(key="1790", value="BØRNS", playlistId=null),
			Item(key="1801", value="Bruce Springsteen", playlistId=null),
			Item(key="1906", value="Fleetwood Mac", playlistId=null),
			Item(key="1961", value="Harvey Danger", playlistId=null),
			Item(key="1962", value="The Head And The Heart", playlistId=null),
			Item(key="1972", value="Hozier", playlistId=null),
			Item(key="1973", value="Hurray For The Riff Raff", playlistId=null),
			Item(key="2221", value="Shakey Graves", playlistId=null),
			Item(key="2222", value="The Shins", playlistId=null),
		)
	}
}
