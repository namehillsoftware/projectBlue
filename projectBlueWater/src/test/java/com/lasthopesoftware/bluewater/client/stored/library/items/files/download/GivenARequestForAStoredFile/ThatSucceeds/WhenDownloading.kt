package com.lasthopesoftware.bluewater.client.stored.library.items.files.download.GivenARequestForAStoredFile.ThatSucceeds

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.*

class WhenDownloading {
	private val responseBytes by lazy {
		val bytes = ByteArray(400)
		Random().nextBytes(bytes)
		bytes
	}

	private val inputStream by lazy {
		val fakeConnectionProvider = FakeConnectionProvider()
		fakeConnectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				200,
				responseBytes
			)
		})
		val libraryConnections = mockk<ProvideLibraryConnections> {
			every { promiseLibraryConnection(LibraryId(4)) } returns ProgressingPromise(fakeConnectionProvider)
		}

		val downloader =
			StoredFileDownloader(ServiceFileUriQueryParamsProvider, libraryConnections)
		ExpiringFuturePromise(
			downloader.promiseDownload(
				LibraryId(4),
				StoredFile().setServiceId(4)
			)
		).get()
	}

	@Test
	fun `then the input stream is returned`() {
		val outputStream = ByteArrayOutputStream()
		IOUtils.copy(inputStream, outputStream)
		assertThat(outputStream.toByteArray()).containsExactly(*responseBytes)
	}
}
