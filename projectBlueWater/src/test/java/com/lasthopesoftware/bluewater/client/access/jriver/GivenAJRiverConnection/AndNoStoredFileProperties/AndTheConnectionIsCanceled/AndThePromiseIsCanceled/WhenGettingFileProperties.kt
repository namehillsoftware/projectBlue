package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndNoStoredFileProperties.AndTheConnectionIsCanceled.AndThePromiseIsCanceled

import com.lasthopesoftware.TestMcwsUrl
import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.MediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.url.MediaCenterUrlBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

class WhenGettingFileProperties {
	companion object {
		private const val serviceFileId = "155"
	}

	private val deferredReject = DeferredPromise<HttpResponse>(IOException("Canceled"))

	private val filePropertiesProvider by lazy {
		MediaCenterConnection(
			ServerConnection(TestUrl),
			FakeHttpConnectionProvider(
				mockk {
					every { promiseResponse(MediaCenterUrlBuilder.buildUrl(TestMcwsUrl, "File/GetInfo", "File=$serviceFileId")) } returns deferredReject
				}
			),
			mockk(),
		)
    }

	private var cancellationException: CancellationException? = null

	@BeforeAll
	fun act() {
		try {
			val promisedFileProperties = filePropertiesProvider
				.promiseFileProperties(ServiceFile(serviceFileId))

			promisedFileProperties.cancel()

			deferredReject.resolve()

			promisedFileProperties.toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			cancellationException = ee.cause as? CancellationException ?: throw ee
		}
	}

    @Test
    fun `then a cancellation exception is thrown`() {
        assertThat(cancellationException).isNotNull()
    }
}
