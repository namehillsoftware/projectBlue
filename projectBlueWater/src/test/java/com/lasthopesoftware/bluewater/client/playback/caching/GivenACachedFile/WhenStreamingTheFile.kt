package com.lasthopesoftware.bluewater.client.playback.caching.GivenACachedFile

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.ICache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier.ICacheStreamSupplier
import com.lasthopesoftware.bluewater.client.playback.caching.DiskFileCacheDataSource
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
import java.io.File
import java.io.FileOutputStream
import java.util.*

@RunWith(RobolectricTestRunner::class)
class WhenStreamingTheFile {
    companion object {
        private val bytesWritten = ByteArray(512 * 1024)
		private val bytesRead = ByteArray(512 * 1024)
        private val bytes by lazy { ByteArray(512 * 1024).also { Random().nextBytes(it) } }
        private var cacheKey: String? = null
        private var committedToCache = false

		@BeforeClass
        @JvmStatic
        fun context() {
			val file = File.createTempFile("M081w", ".tmp")
			try {
				FileOutputStream(file).use { it.write(bytes) }

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
									return Promise(CachedFile())
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

				val diskFileCacheDataSource = DiskFileCacheDataSource(
					dataSource,
					fakeCacheStreamSupplier,
					mockk<ICache>().apply {
						every { promiseCachedFile("/file?ID=876:0:-1") } returns Promise(file)
					}
				)

				diskFileCacheDataSource.open(
					DataSpec.Builder()
						.setUri(Uri.parse("http://my-server/file?ID=876"))
						.setPosition(0)
						.setLength(-1L)
						.setKey("waiter")
						.build()
				)

				do {
					val readResult = diskFileCacheDataSource.read(bytesRead, 0, bytesRead.size)
				} while (readResult != C.RESULT_END_OF_INPUT)
			} finally {
			    file.delete()
			}
        }
    }

    @Test
    fun `then the file is not written because it is already cached`() {
        assertThat(bytesWritten).containsOnly(0)
    }

	@Test
	fun `then the file is read correctly`() {
		assertArrayEquals(bytes, bytesRead)
	}

    @Test
    fun `then the file is not cached`() {
        assertThat(committedToCache).isFalse
    }
}
