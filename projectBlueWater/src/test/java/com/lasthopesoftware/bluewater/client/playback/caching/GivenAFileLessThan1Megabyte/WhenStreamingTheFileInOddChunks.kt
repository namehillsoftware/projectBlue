package com.lasthopesoftware.bluewater.client.playback.caching.GivenAFileLessThan1Megabyte

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.EntireFileCachedDataSource
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
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
		private const val libraryId = 393
		private var writtenLibraryId: LibraryId? = null
		private val bytesWritten = ByteArray(512 * 1024)
		private val bytes by lazy { ByteArray(512 * 1024).also { Random().nextBytes(it) } }
		private var committedToCache = false

		@BeforeClass
		@JvmStatic
		fun context() {
			val deferredCommit = DeferredPromise(CachedFile() as CachedFile?)

			val fakeCacheStreamSupplier =
				object : SupplyCacheStreams {
					override fun promiseCachedFileOutputStream(libraryId: LibraryId, uniqueKey: String): Promise<CacheOutputStream> {
						writtenLibraryId = libraryId
						return Promise<CacheOutputStream>(object : CacheOutputStream {
							var numberOfBytesWritten = 0
							override fun promiseWrite(
								buffer: ByteArray,
								offset: Int,
								length: Int
							): Promise<CacheOutputStream> =
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

							override fun commitToCache(): Promise<CachedFile?> {
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
			val dataSource = mockk<HttpDataSource>(relaxUnitFun = true).apply {
				every { open(any()) } returns bytes.size.toLong()

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
				LibraryId(libraryId),
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
			val random = Random()
			var readResult: Int
			do {
				val bytes = ByteArray(random.nextInt(1000000))
				readResult = diskFileCacheDataSource.read(bytes, 0, bytes.size)
			} while (readResult != C.RESULT_END_OF_INPUT)
			diskFileCacheDataSource.close()

			deferredCommit.toExpiringFuture().get()
		}
	}

	@Test
	fun `then the correct library is written to`() {
		assertThat(writtenLibraryId?.id).isEqualTo(libraryId)
	}

	@Test
	fun thenTheEntireFileIsWritten() {
		assertArrayEquals(bytes, bytesWritten)
	}

	@Test
	fun thenTheFileIsCached() {
		assertThat(committedToCache).isTrue
	}
}
