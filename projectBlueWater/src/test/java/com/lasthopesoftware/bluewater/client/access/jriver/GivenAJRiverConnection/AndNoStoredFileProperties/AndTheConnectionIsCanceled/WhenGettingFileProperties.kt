package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndNoStoredFileProperties.AndTheConnectionIsCanceled

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
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
		private const val serviceFileId = "31"
	}

	private val filePropertiesProvider by lazy {
        LiveMediaCenterConnection(
			MediaCenterConnectionDetails(TestUrl),
			FakeHttpConnectionProvider(mockk {
				every { promiseResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "File/GetInfo", "File=$serviceFileId")) } returns Promise(IOException("Canceled"))
			}),
			mockk(),
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
