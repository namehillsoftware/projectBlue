package com.lasthopesoftware.bluewater.client.stored.library.items.files.download.GivenARequestForAStoredFile.ThatSucceeds

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

class WhenDownloading {
	companion object {
		private val responseBytes by lazy {
			val bytes = ByteArray(400)
			Random().nextBytes(bytes)
			bytes
		}
		private var inputStream: InputStream? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val fakeConnectionProvider = FakeConnectionProvider()
			fakeConnectionProvider.mapResponse({
				FakeConnectionResponseTuple(
					200,
					responseBytes
				)
			})
			val libraryConnections = Mockito.mock(
				ProvideLibraryConnections::class.java
			)
			Mockito.`when`(libraryConnections.promiseLibraryConnection(LibraryId(4)))
				.thenReturn(ProgressingPromise(fakeConnectionProvider))
			val downloader =
				StoredFileDownloader(ServiceFileUriQueryParamsProvider(), libraryConnections)
			inputStream = FuturePromise(
				downloader.promiseDownload(
					LibraryId(4),
					StoredFile().setServiceId(4)
				)
			).get()
		}
	}

	@Test
	@Throws(IOException::class)
	fun thenTheInputStreamIsReturned() {
		val outputStream = ByteArrayOutputStream()
		IOUtils.copy(inputStream, outputStream)
		Assertions.assertThat(outputStream.toByteArray()).containsExactly(*responseBytes)
	}
}
