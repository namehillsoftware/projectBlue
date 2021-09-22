package com.lasthopesoftware.bluewater.client.stored.library.items.files.download.GivenARequestForAStoredFile.ThatReturnsNull

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.io.InputStream

class WhenDownloading {
	@Test
	fun thenAnEmptyInputStreamIsReturned() {
		assertThat(inputStream!!.available()).isEqualTo(0)
	}

	companion object {
		private var inputStream: InputStream? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val builder = Request.Builder()
			builder.url("http://stuff/")
			val responseBuilder = Response.Builder()
			responseBuilder
				.request(builder.build())
				.protocol(Protocol.HTTP_1_1)
				.code(202)
				.message("Not Found")
				.body(null)
			val fakeConnectionProvider = mockk<IConnectionProvider>()
			every { fakeConnectionProvider.promiseResponse(*anyVararg()) } returns responseBuilder.build().toPromise()

			val libraryConnections = mockk<ProvideLibraryConnections>()
			every { libraryConnections.promiseLibraryConnection(LibraryId(4)) } returns ProgressingPromise(fakeConnectionProvider)

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
}
