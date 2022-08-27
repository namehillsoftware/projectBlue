package com.lasthopesoftware.bluewater.client.stored.library.items.files.download.GivenARequestForAStoredFile.ThatReturnsA404

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenDownloading {
	private val inputStream by lazy {
		val fakeConnectionProvider =
			FakeLibraryConnectionProvider(object : HashMap<LibraryId, IConnectionProvider>() {
				init {
					put(LibraryId(2), FakeConnectionProvider())
				}
			})
		val downloader =
			StoredFileDownloader(ServiceFileUriQueryParamsProvider, fakeConnectionProvider)
		ExpiringFuturePromise(
			downloader.promiseDownload(
				LibraryId(2),
				StoredFile().setServiceId(4)
			)
		).get()
	}

	@Test
	fun `then an empty input stream is returned`() {
		assertThat(inputStream!!.available()).isEqualTo(0)
	}
}
