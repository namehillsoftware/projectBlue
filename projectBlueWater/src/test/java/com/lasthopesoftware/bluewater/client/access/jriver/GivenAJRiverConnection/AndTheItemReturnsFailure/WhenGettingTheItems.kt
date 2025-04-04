package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndTheItemReturnsFailure

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.MediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

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
					"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<Response Status="Failure"/>""".encodeToByteArray().inputStream()
				)
			}
		}

		MediaCenterConnection(
			ServerConnection(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)
	}

	private var exception: IOException? = null

	@BeforeAll
	fun act() {
		try {
			mut.promiseItems(ItemId(itemId)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e.cause as? IOException
		}
	}

	@Test
	fun `then an exception is thrown`() {
		assertThat(exception?.message).isEqualTo("Server returned 'Failure'.")
	}
}
