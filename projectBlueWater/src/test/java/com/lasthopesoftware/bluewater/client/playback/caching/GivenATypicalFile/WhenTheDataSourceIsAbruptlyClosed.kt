package com.lasthopesoftware.bluewater.client.playback.caching.GivenATypicalFile

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.EntireFileCachedDataSource
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
import okio.BufferedSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class WhenTheDataSourceIsAbruptlyClosed {
	companion object {
		private var bytes: Lazy<ByteArray>? = lazy { ByteArray(7 * 1024 * 1024).also { Random().nextBytes(it) } }
		private var committedToCache = false

		@BeforeClass
		@JvmStatic
		fun context() {
			val fakeCacheStreamSupplier =
				object : SupplyCacheStreams {
					override fun promiseCachedFileOutputStream(libraryId: LibraryId, uniqueKey: String): Promise<CacheOutputStream> {
						return Promise<CacheOutputStream>(object : CacheOutputStream {
							var numberOfBytesWritten = 0
							val bytesWritten = ByteArray(7 * 1024 * 1024)
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
			bytes?.value?.apply(buffer::write)
			val dataSource = mockk<HttpDataSource>(relaxed = true).apply {
				every { read(any(), any(), any()) } answers {
					var bytesRead = 0
					val bytesToRead = arg<Int>(2)
					var bytesRemaining = bytesToRead
					var offset = arg<Int>(1)
					while (bytesRead < bytesToRead) {
						val bufferRead = buffer.read(arg(0), offset, bytesRemaining)
						if (bufferRead == -1) {
							bytesRead = -1
							break
						}
						bytesRead += bufferRead
						offset += bufferRead
						bytesRemaining -= bufferRead
					}
					bytesRead
				}
			}

			val diskFileCacheDataSource = EntireFileCachedDataSource(
				LibraryId(697),
				dataSource,
				fakeCacheStreamSupplier
			)
			diskFileCacheDataSource.open(
				DataSpec.Builder()
					.setUri(Uri.parse("http://my-server/file"))
					.setPosition(0)
					.setLength(C.LENGTH_UNSET.toLong()).setKey("1")
					.build()
			)

			val random = Random()
			var byteCount = 0
			do {
				val bytes = ByteArray(random.nextInt(1000000))
				val readResult = diskFileCacheDataSource.read(bytes, 0, bytes.size)
				if (readResult == C.RESULT_END_OF_INPUT) break
				byteCount += readResult
			} while (byteCount < 3 * 1024 * 1024)
			diskFileCacheDataSource.close()
		}

		@JvmStatic
		@AfterClass
		fun cleanup() {
			bytes = null
		}
	}

	@Test
	fun thenTheFileIsNotCached() {
		assertThat(committedToCache).isFalse
	}
}
