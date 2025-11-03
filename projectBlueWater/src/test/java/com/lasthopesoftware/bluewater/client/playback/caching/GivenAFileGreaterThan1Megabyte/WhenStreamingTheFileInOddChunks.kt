package com.lasthopesoftware.bluewater.client.playback.caching.GivenAFileGreaterThan1Megabyte

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CacheWritableStream
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.EntireFileCachedDataSource
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Random

private const val libraryId = 568

@RunWith(RobolectricTestRunner::class)
class WhenStreamingTheFileInOddChunks {
    companion object {
        private var bytesWritten: ByteArray? = ByteArray(7 * 1024 * 1024)
        private var bytes: Lazy<ByteArray>? = lazy { ByteArray(7 * 1024 * 1024).also { Random().nextBytes(it) } }
        private var cacheKey: String? = null
        private var committedToCache = false

		@BeforeClass
        @JvmStatic
        fun context() {
			val deferredCommit = DeferredPromise(CachedFile() as CachedFile?)

			val fakeCacheStreamSupplier =
				object : SupplyCacheStreams {
					override fun promiseCachedFileOutputStream(libraryId: LibraryId, uniqueKey: String): Promise<CacheWritableStream> {
						cacheKey = uniqueKey
						return Promise<CacheWritableStream>(object : CacheWritableStream {
							var numberOfBytesWritten = 0
							override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<Int> {
								val bytesWritten = bytesWritten ?: return numberOfBytesWritten.toPromise()
								if (numberOfBytesWritten < bytesWritten.size) {
									buffer.copyInto(
										bytesWritten,
										numberOfBytesWritten,
										offset,
										(offset + bytesWritten.size - numberOfBytesWritten).coerceAtMost(offset + length)
									)
									numberOfBytesWritten += length
								}
								return numberOfBytesWritten.toPromise()
							}

                            override fun commitToCache(): Promise<CachedFile?> {
								committedToCache = true
								deferredCommit.resolve()
								return deferredCommit
							}

							override fun promiseFlush(): Promise<Unit> {
								return Unit.toPromise()
							}

							override fun promiseClose(): Promise<Unit> = Unit.toPromise()
						})
					}
				}
            val buffer = Buffer()
			bytes?.value?.apply(buffer::write)
            val dataSource = mockk<HttpDataSource>(relaxUnitFun = true).apply {
				every { open(any()) } returns (bytes?.value?.size?.toLong() ?: 0L)

            	every { read(any(), any(), any()) } answers {
					var bytesRead = 0
					val bytesToRead = arg<Int>(2)
					val offset = arg<Int>(1)
					while (bytesRead < bytesToRead) {
						val bufferRead = buffer.read(arg(0), offset + bytesRead, bytesToRead - bytesRead)
						if (bufferRead == -1) {
							if (bytesRead == 0) bytesRead = -1
							break
						}

						bytesRead += bufferRead
					}
					bytesRead
				}
			}
            val diskFileCacheDataSource = EntireFileCachedDataSource(
				LibraryId(libraryId),
				dataSource,
				fakeCacheStreamSupplier
			)
            diskFileCacheDataSource.open(
				DataSpec.Builder()
					.setUri(Uri.parse("http://my-server/file?ID=1"))
					.setPosition(0)
					.setLength(C.LENGTH_UNSET.toLong())
					.setKey("hi")
					.build()
            )
            val random = Random()
            var readResult: Int
            do {
                val bytes = ByteArray(random.nextInt(1000000))
                readResult = diskFileCacheDataSource.read(bytes, 0, bytes.size)
            } while (readResult != C.RESULT_END_OF_INPUT)

			deferredCommit.toExpiringFuture().get()
        }

		@JvmStatic
		@AfterClass
		fun cleanup() {
			bytesWritten = null
			bytes = null
		}
    }

    @Test
    fun thenTheEntireFileIsWritten() {
        assertArrayEquals(bytes?.value, bytesWritten)
    }

    @Test
    fun thenTheKeyIsCorrect() {
        assertThat(cacheKey).isEqualToIgnoringCase("/file?ID=1")
    }

    @Test
    fun thenTheFileIsCached() {
        assertThat(committedToCache).isTrue
    }
}
