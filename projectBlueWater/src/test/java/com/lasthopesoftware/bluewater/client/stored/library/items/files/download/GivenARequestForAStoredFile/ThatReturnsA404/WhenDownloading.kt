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
            val fakeConnectionProvider =
                FakeLibraryConnectionProvider(object : HashMap<LibraryId?, IConnectionProvider?>() {
                    init {
                        put(LibraryId(2), FakeConnectionProvider())
                    }
                })
            val downloader =
                StoredFileDownloader(ServiceFileUriQueryParamsProvider, fakeConnectionProvider)
            inputStream = ExpiringFuturePromise(
                downloader.promiseDownload(
                    LibraryId(2),
                    StoredFile().setServiceId(4)
                )
            ).get()
        }
    }
}
