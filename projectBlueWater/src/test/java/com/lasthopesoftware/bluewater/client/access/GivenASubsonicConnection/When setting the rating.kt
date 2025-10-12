package com.lasthopesoftware.bluewater.client.access.GivenASubsonicConnection

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When setting the rating` {
	private val mut by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(TestUrl.withSubsonicApi().addPath("setRating").addParams( "f=json", "id=3933b6ef-602c-409d-b33d-17a187bbbf3e", "rating=4")) {
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
					every { getServerClient(any<SubsonicConnectionDetails>()) } returns httpConnection
				},
                JsonEncoderDecoder,
				mockk(),
			)
		)
	}

	@BeforeAll
	fun act() {
		mut.second
			.promiseFilePropertyUpdate(
				ServiceFile("3933b6ef-602c-409d-b33d-17a187bbbf3e"),
				property = NormalizedFileProperties.Rating,
				value = "4",
				isFormatted = false,
			)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the rating is set correctly correctly`() {
		assertThat(mut.first.recordedRequests).contains(
			TestUrl
				.withSubsonicApi()
				.addPath("setRating").addParams( "f=json", "id=3933b6ef-602c-409d-b33d-17a187bbbf3e", "rating=4"),
		)
	}
}
