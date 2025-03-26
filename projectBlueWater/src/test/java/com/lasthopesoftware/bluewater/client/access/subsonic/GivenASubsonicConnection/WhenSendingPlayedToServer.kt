package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.emptyByteArray
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSendingPlayedToServer {

	private var isFilePlayedCalled = false
	private val updater by lazy {
		LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "P4PSyKKGhNR", "l048RtrdOV"),
			FakeHttpConnectionProvider(
				FakeHttpConnection().apply {
					mapResponse(TestUrl.withSubsonicApi().addPath( "scrobble").addParams("id=8d57775ff6504d009e68c1de54d6990a")) {
						isFilePlayedCalled = true
						PassThroughHttpResponse(
							200,
							"OK",
							emptyByteArray.inputStream()
						)
					}
				}
			),
			mockk(),
		)
	}

	@BeforeAll
	fun act() {
		updater.promisePlaystatsUpdate(ServiceFile("8d57775ff6504d009e68c1de54d6990a")).toExpiringFuture().get()
	}

	@Test
	fun `then the file is updated`() {
		assertThat(isFilePlayedCalled).isTrue
	}
}
