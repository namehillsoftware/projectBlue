package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndNoId

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.items.Item
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

class `When getting items` {

	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath("getIndexes")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"""
						{"subsonic-response":{"status":"ok","version":"1.16.1","type":"navidrome","serverVersion":"0.53.3 (13af8ed4)","openSubsonic":true,"indexes":{"index":[{"name":"#","artist":[{"id":"3ad1c4d579750f99ecb367ddebe46590","name":"22-20s","albumCount":1,"coverArt":"ar-3ad1c4d579750f99ecb367ddebe46590_0","artistImageUrl":"http://media-pc:4533/share/img/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImFyLTNhZDFjNGQ1Nzk3NTBmOTllY2IzNjdkZGViZTQ2NTkwXzAiLCJpc3MiOiJORCJ9.4rWuh1vusw_nmCeHdUxmA8tJYq_8Zsfg7Mp_yLAsEvE?size=600"},{"id":"2f3317c0780aff2f6d6bdca89f4b9822","name":"22‐20s","albumCount":1,"coverArt":"ar-2f3317c0780aff2f6d6bdca89f4b9822_0","artistImageUrl":"http://media-pc:4533/share/img/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImFyLTJmMzMxN2MwNzgwYWZmMmY2ZDZiZGNhODlmNGI5ODIyXzAiLCJpc3MiOiJORCJ9.08d8q-tSqVQlCxygFZDfcppZh2YOgJKCGZojplE-DFk?size=600"},{"id":"cf70c8e7363ca943a5b80d2b95517113","name":"3 Doors Down","albumCount":2,"coverArt":"ar-cf70c8e7363ca943a5b80d2b95517113_0","artistImageUrl":"http://media-pc:4533/share/img/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImFyLWNmNzBjOGU3MzYzY2E5NDNhNWI4MGQyYjk1NTE3MTEzXzAiLCJpc3MiOiJORCJ9.cgKM_BiuK1GmwbWWfNYFtCc8mnzva4AbSV5SFoqXtEo?size=600"},{"id":"9dfcd5e558dfa04aaf37f137a1d9d3e5","name":"311","albumCount":6,"coverArt":"ar-9dfcd5e558dfa04aaf37f137a1d9d3e5_0","artistImageUrl":"http://media-pc:4533/share/img/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImFyLTlkZmNkNWU1NThkZmEwNGFhZjM3ZjEzN2ExZDlkM2U1XzAiLCJpc3MiOiJORCJ9.hrI8Ndbn9CycTNohVa3mfRoIxqIYfF2E3G-I9ZrPXD8?size=600"},{"id":"d27e9ceac68a17257c85a4d433e7a4c8","name":"50 Cent","albumCount":6,"coverArt":"ar-d27e9ceac68a17257c85a4d433e7a4c8_0","artistImageUrl":"http://media-pc:4533/share/img/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImFyLWQyN2U5Y2VhYzY4YTE3MjU3Yzg1YTRkNDMzZTdhNGM4XzAiLCJpc3MiOiJORCJ9.8qOoqM6f_DL1lAVDQGxpgzfrpsIn9BxcxhZqdeon5l8?size=600"}]},{"name":"A","artist":[{"id":"563727e73731392260da4a787f534387","name":"A.A. Bondy","albumCount":8,"coverArt":"ar-563727e73731392260da4a787f534387_0","artistImageUrl":"http://media-pc:4533/share/img/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImFyLTU2MzcyN2U3MzczMTM5MjI2MGRhNGE3ODdmNTM0Mzg3XzAiLCJpc3MiOiJORCJ9.bXPjfLzco49ANwNuJcy7-rqN0wfgS1qfPsX5m0e-Cjw?size=600"}]},{"name":"G","artist":[{"id":"38ec9eabef4778ac77923ad3a59a23f9","name":"Gustavo Santaolalla","albumCount":2,"coverArt":"ar-38ec9eabef4778ac77923ad3a59a23f9_0","artistImageUrl":"http://media-pc:4533/share/img/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImFyLTM4ZWM5ZWFiZWY0Nzc4YWM3NzkyM2FkM2E1OWEyM2Y5XzAiLCJpc3MiOiJORCJ9.F8rZysxVYm8eIgA03qTSGm7_Y0Uw_ViAVRHa4nWsZp4?size=600"}]},{"name":"V","artist":[{"id":"03b645ef2100dfc42fa9785ea3102295","name":"Various Artists","albumCount":1,"coverArt":"ar-03b645ef2100dfc42fa9785ea3102295_0","artistImageUrl":"http://media-pc:4533/share/img/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImFyLTAzYjY0NWVmMjEwMGRmYzQyZmE5Nzg1ZWEzMTAyMjk1XzAiLCJpc3MiOiJORCJ9.hL-QUehCXl4rA8cJcaNYXOfoHiz46906Dd-7ykCCDxQ?size=600"}]}],"lastModified":1743978082001,"ignoredArticles":"The El La Los Las Le Les Os As O A"}}}
					""".encodeToByteArray().inputStream()
				)
			}
		}

		LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "Xtbyp9lhSJY", "WVe5KSj"),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)
	}

	private var items = emptyList<Item>()

	@BeforeAll
	fun act() {
		items = mut.promiseItems(null).toExpiringFuture().get()!!
	}

	@Test
	fun `then the items are correct`() {
		assertThat(items).containsExactly(
			Item(key = "3ad1c4d579750f99ecb367ddebe46590", value = "22-20s", playlistId = null),
			Item(key = "2f3317c0780aff2f6d6bdca89f4b9822", value = "22‐20s", playlistId = null),
			Item(key = "cf70c8e7363ca943a5b80d2b95517113", value = "3 Doors Down", playlistId = null),
			Item(key = "9dfcd5e558dfa04aaf37f137a1d9d3e5", value = "311", playlistId = null),
			Item(key = "d27e9ceac68a17257c85a4d433e7a4c8", value = "50 Cent", playlistId = null),
			Item(key = "563727e73731392260da4a787f534387", value = "A.A. Bondy", playlistId = null),
			Item(key = "38ec9eabef4778ac77923ad3a59a23f9", value = "Gustavo Santaolalla", playlistId = null),
			Item(key = "03b645ef2100dfc42fa9785ea3102295", value = "Various Artists", playlistId = null),
		)
	}
}
