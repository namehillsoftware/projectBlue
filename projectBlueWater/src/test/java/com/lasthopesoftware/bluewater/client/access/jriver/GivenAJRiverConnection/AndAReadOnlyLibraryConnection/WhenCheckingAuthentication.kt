package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAReadOnlyLibraryConnection

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.MediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenCheckingAuthentication {

	private val isReadOnly by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "Authenticate")) {
				PassThroughHttpResponse(
					200,
					"OK",
					(
						"""<Response Status="OK">
<Item Name="Token">B9yXQtTL</Item>
<Item Name="ReadOnly">1</Item>
<Item Name="PreLicensed">0</Item>
</Response>""").toByteArray().inputStream()
				)
			}
		}

		val access = MediaCenterConnection(
			ServerConnection(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
			mockk(),
		)
		access.promiseIsReadOnly().toExpiringFuture().get()
	}

	@Test
	fun `then the connection is read only`() {
		assertThat(isReadOnly).isTrue
	}
}
