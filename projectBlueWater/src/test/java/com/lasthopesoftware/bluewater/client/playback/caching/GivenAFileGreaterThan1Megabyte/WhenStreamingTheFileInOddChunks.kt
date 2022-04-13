package com.lasthopesoftware.bluewater.client.playback.caching.GivenAFileGreaterThan1Megabyte

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.ICache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier.ICacheStreamSupplier
import com.lasthopesoftware.bluewater.client.playback.caching.DiskFileCacheDataSource
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
import okio.BufferedSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class WhenStreamingTheFileInOddChunks {
    companion object {
        private val bytesWritten = ByteArray(7 * 1024 * 1024)
        private val bytes by lazy { ByteArray(7 * 1024 * 1024).also { Random().nextBytes(it) } }
        private var cacheKey: String? = null
        private var committedToCache = false

		@BeforeClass
        @JvmStatic
        fun context() {
			val deferredCommit = DeferredPromise(CachedFile())

			val fakeCacheStreamSupplier =
				object : ICacheStreamSupplier {
					override fun promiseCachedFileOutputStream(uniqueKey: String): Promise<CacheOutputStream> {
						cacheKey = uniqueKey
						return Promise<CacheOutputStream>(object : CacheOutputStream {
							var numberOfBytesWritten = 0
							override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<CacheOutputStream> =
								Promise<CacheOutputStream>(this)

							override fun promiseTransfer(bufferedSource: BufferedSource): Promise<CacheOutputStream> {
								while (numberOfBytesWritten < bytesWritten.size) {
									val read = bufferedSource.read(
										bytesWritten,
										numberOfBytesWritten,
										bytesWritten.size - numberOfBytesWritten
									)
									if (read == -1) return Promise<CacheOutputStream>(this)
									numberOfBytesWritten += read
								}
								return Promise<CacheOutputStream>(this)
							}

							override fun commitToCache(): Promise<CachedFile> {
								committedToCache = true
								deferredCommit.resolve()
								return deferredCommit
							}

							override fun flush(): Promise<CacheOutputStream> {
								return Promise<CacheOutputStream>(this)
							}

							override fun close() {}
						})
					}
				}
            val buffer = Buffer()
            buffer.write(bytes)
            val dataSource = mockk<HttpDataSource>(relaxed = true).apply {
            	every { read(any(), any(), any()) } answers {
					var bytesRead = 0
					val bytesToRead = arg<Int>(2)
					var bytesRemaining = bytesToRead
					var offset = arg<Int>(1)
					while (bytesRead < bytesToRead) {
						val bufferRead = buffer.read(arg(0), offset, bytesRemaining)
						if (bufferRead == -1) {
							if (bytesRead == 0) bytesRead = -1
							break
						}
						bytesRead += bufferRead
						offset += bufferRead
						bytesRemaining -= bufferRead
					}
					bytesRead
				}
			}
            val diskFileCacheDataSource = DiskFileCacheDataSource(
                dataSource,
                fakeCacheStreamSupplier,
				mockk<ICache>().apply {
					every { promiseCachedFile(any()) } returns Promise.empty()
				}
            )
            diskFileCacheDataSource.open(
				DataSpec.Builder()
					.setUri(Uri.parse("http://my-server/file?ID=1"))
					.setPosition(0)
					.setLength((7 * 1024 * 1024).toLong())
					.setKey("hi")
					.build()
            )
            val random = Random()
            var readResult: Int
            do {
                val bytes = ByteArray(random.nextInt(1000000))
                readResult = diskFileCacheDataSource.read(bytes, 0, bytes.size)
            } while (readResult != C.RESULT_END_OF_INPUT)

			deferredCommit.toFuture().get()
        }
    }

    @Test
    fun thenTheEntireFileIsWritten() {
        assertArrayEquals(bytes, bytesWritten)
    }

    @Test
    fun thenTheKeyIsCorrect() {
        assertThat(cacheKey).isEqualToIgnoringCase("/file?ID=1:0:7340032")
    }

    @Test
    fun thenTheFileIsCached() {
        assertThat(committedToCache).isTrue
    }
}
