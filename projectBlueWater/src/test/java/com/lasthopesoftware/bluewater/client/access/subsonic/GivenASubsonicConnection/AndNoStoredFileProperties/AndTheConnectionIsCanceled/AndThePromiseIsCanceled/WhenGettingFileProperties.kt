package com.lasthopesoftware.bluewater.client.access.subsonic.GivenASubsonicConnection.AndNoStoredFileProperties.AndTheConnectionIsCanceled.AndThePromiseIsCanceled

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveSubsonicConnection
import com.lasthopesoftware.bluewater.client.connection.requests.FakeHttpConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
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
		private const val serviceFileId = "fb459e70d9e349f2b12c8c4ef3773962"
	}

	private val deferredReject = DeferredPromise<HttpResponse>(IOException("Canceled"))

	private val filePropertiesProvider by lazy {
		LiveSubsonicConnection(
			SubsonicConnectionDetails(TestUrl, "4Vgsz0MKH3", "c3ZtnWx2", "bE98VYcj1D"),
			FakeHttpConnectionProvider(
				mockk {
					every { promiseResponse(TestUrl.withSubsonicApi().addParams("getSong").addParams("id=$serviceFileId")) } returns deferredReject
				}
			),
			mockk(),
			JsonEncoderDecoder,
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
