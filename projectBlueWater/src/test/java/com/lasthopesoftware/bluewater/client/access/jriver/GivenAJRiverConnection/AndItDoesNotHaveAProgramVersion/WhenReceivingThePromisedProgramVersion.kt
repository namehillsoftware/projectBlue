package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItDoesNotHaveAProgramVersion

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.connection.JRiverLibraryConnection
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.JRiverUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenReceivingThePromisedProgramVersion {
    private val version by lazy {
		val httpConnection = FakeHttpConnection().apply {
			mapResponse(JRiverUrlBuilder.getUrl(TestMcwsUrl, "Alive")) {
				PassThroughHttpResponse(
					200,
					"OK",
					"<Response Status=\"OK\"></Response>".toByteArray().inputStream()
				)
			}
		}

		val access = JRiverLibraryConnection(
			ServerConnection(TestUrl),
			FakeHttpConnectionProvider(httpConnection),
		)
		access.promiseServerVersion().toExpiringFuture().get()
	}

    @Test
    fun `then the server version is null`() {
        assertThat(version).isNull()
    }
}
