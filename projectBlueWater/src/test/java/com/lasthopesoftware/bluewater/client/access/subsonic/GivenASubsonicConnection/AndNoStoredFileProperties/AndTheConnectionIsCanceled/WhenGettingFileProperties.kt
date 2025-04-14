package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndNoStoredFileProperties.AndTheConnectionIsCanceled

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

class WhenGettingFileProperties {
	companion object {
		private const val serviceFileId = "a2ab9993f67a48289c77b401b2f961b9"
	}

	private val filePropertiesProvider by lazy {
		LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "1mKKuU0lp", "Kt9syLo"),
			FakeHttpConnectionProvider(mockk {
				every { promiseResponse(TestUrl.withSubsonicApi().addPath("getSong").addParams("id=$serviceFileId")) } returns Promise(IOException("Canceled"))
			}),
			mockk(),
			JsonEncoderDecoder,
		)
    }

	private var ioException: IOException? = null

	@BeforeAll
	fun act() {
		try {
			filePropertiesProvider
				.promiseFileProperties(ServiceFile(serviceFileId))
				.toExpiringFuture()
				.get()
		} catch (ee: ExecutionException) {
			ioException = ee.cause as? IOException
		}
	}

    @Test
    fun `then the exception is thrown`() {
        assertThat(ioException?.message).isEqualTo("Canceled")
    }
}
