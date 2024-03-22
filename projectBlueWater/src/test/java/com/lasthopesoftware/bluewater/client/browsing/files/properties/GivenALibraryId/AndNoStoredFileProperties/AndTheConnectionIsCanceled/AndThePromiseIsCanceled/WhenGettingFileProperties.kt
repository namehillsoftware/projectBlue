package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenALibraryId.AndNoStoredFileProperties.AndTheConnectionIsCanceled.AndThePromiseIsCanceled

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeRevisionConnectionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

private const val libraryId = 150
private const val serviceFileId = 155

class WhenGettingFileProperties {
	private val deferredReject = DeferredPromise<Response>(IOException("Canceled"))

	private val filePropertiesProvider by lazy {
        val fakeFileConnectionProvider = spyk<FakeRevisionConnectionProvider> {
			every { promiseResponse("File/GetInfo", "File=$serviceFileId") } returns deferredReject
		}
        val fakeLibraryConnectionProvider =
            FakeLibraryConnectionProvider(mapOf(Pair(LibraryId(libraryId), fakeFileConnectionProvider)))
        FilePropertiesProvider(
			GuaranteedLibraryConnectionProvider(fakeLibraryConnectionProvider),
            LibraryRevisionProvider(fakeLibraryConnectionProvider),
            mockk(relaxed = true)
        )
    }

	private var cancellationException: CancellationException? = null

	@BeforeAll
	fun act() {
		try {
			val promisedFileProperties = filePropertiesProvider
				.promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId))

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
