package com.lasthopesoftware.bluewater.client.stored.library.items.files.download.GivenARequestForAStoredFile.ThatReturnsNull

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenDownloading {
	private val inputStream by lazy {
		val builder = Request.Builder()
		builder.url("http://stuff/")
		val responseBuilder = Response.Builder()
		responseBuilder
			.request(builder.build())
			.protocol(Protocol.HTTP_1_1)
			.code(202)
			.message("Not Found")
			.body("".toResponseBody())
		val fakeConnectionProvider = mockk<IConnectionProvider> {
			every { promiseResponse(*anyVararg()) } returns responseBuilder.build().toPromise()
		}

		val libraryConnections = mockk<ProvideLibraryConnections> {
			every { promiseLibraryConnection(LibraryId(4)) } returns ProgressingPromise(fakeConnectionProvider)
		}

		val downloader = StoredFileDownloader(ServiceFileUriQueryParamsProvider, libraryConnections)
		downloader.promiseDownload(LibraryId(4), StoredFile().setServiceId(4)).toExpiringFuture().get()
	}

	@Test
	fun `then an empty input stream is returned`() {
		assertThat(inputStream!!.available()).isEqualTo(0)
	}
}
