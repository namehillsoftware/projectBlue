package com.lasthopesoftware.bluewater.client.playback.caching.GivenAFileLessThan1Megabyte.AndCacheSizeIsLessThanRemoteSize

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.EntireFileCachedDataSource
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
import okio.BufferedSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Random

@RunWith(RobolectricTestRunner::class)
class WhenStreamingTheFile {
	companion object {
		private const val fileSize = 512 * 1024
		private var bytes: Lazy<ByteArray>? = lazy { ByteArray(512 * 1024).also { Random().nextBytes(it) } }
		private var bytesRead: ByteArray? = ByteArray(fileSize)
		private var bytesWritten: ByteArray? = ByteArray(fileSize)
		private var closedBeforeCommittingToCache = false

		@BeforeClass
		@JvmStatic
		fun context() {
			val deferredCommit = DeferredPromise(CachedFile() as CachedFile?)

			val fakeCacheStreamSupplier = object : SupplyCacheStreams {
				private var committedToCache = false

				override fun promiseCachedFileOutputStream(libraryId: LibraryId, uniqueKey: String): Promise<CacheOutputStream> {
					return Promise<CacheOutputStream>(object : CacheOutputStream {
						var numberOfBytesWritten = 0

						override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<CacheOutputStream> =
							Promise<CacheOutputStream>(this)

						override fun promiseTransfer(bufferedSource: BufferedSource): Promise<CacheOutputStream> {
							bytesWritten?.also {
								while (numberOfBytesWritten < it.size) {
									val read = bufferedSource.read(
										it,
										numberOfBytesWritten,
										it.size - numberOfBytesWritten
									)
									if (read == -1) return Promise<CacheOutputStream>(this)
									numberOfBytesWritten += read
								}
							}
							return Promise<CacheOutputStream>(this)
						}

						override fun commitToCache(): Promise<CachedFile?> {
							committedToCache = true
							deferredCommit.resolve()
							return deferredCommit
						}

						override fun flush(): Promise<CacheOutputStream> = Promise<CacheOutputStream>(this)

						override fun close() {
							closedBeforeCommittingToCache = !committedToCache
						}
					})
				}
			}

			val buffer = Buffer()
			bytes?.value?.apply(buffer::write)
			val dataSource = mockk<HttpDataSource>(relaxUnitFun = true).apply {
				every { open(any()) } returns fileSize * 2L // expect a large file size

				every { read(any(), any(), any()) } answers {
					val bytesToRead = arg<Int>(2)
					val offset = arg<Int>(1)
					var bytesRead = 0
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
				LibraryId(868),
                dataSource,
                fakeCacheStreamSupplier,
			)
			diskFileCacheDataSource.open(
				DataSpec.Builder()
					.setUri(Uri.parse("http://my-server/file"))
					.setPosition(0)
					.setLength(C.LENGTH_UNSET.toLong()).setKey("1")
					.build()
			)
			bytesRead?.also {
				do {
					val readResult = diskFileCacheDataSource.read(it, 0, it.size)
				} while (readResult != C.RESULT_END_OF_INPUT)
			}
			diskFileCacheDataSource.close()
		}

		@JvmStatic
		@AfterClass
		fun cleanup() {
			bytesRead = null
			bytesWritten = null
			bytes = null
		}
	}

	@Test
	fun `then the file is not written`() {
		assertThat(bytesWritten).containsOnly(0)
	}

	@Test
	fun `then the file is not cached`() {
		assertThat(closedBeforeCommittingToCache).isTrue
	}

	@Test
	fun `then the file is read correctly`() {
		assertArrayEquals(bytes?.value, bytesRead)
	}

}
