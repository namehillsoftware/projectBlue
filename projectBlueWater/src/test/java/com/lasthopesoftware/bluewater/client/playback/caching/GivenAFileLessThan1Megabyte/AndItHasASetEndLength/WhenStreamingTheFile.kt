package com.lasthopesoftware.bluewater.client.playback.caching.GivenAFileLessThan1Megabyte.AndItHasASetEndLength

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.CacheFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.client.playback.caching.DiskFileCacheDataSource
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
class WhenStreamingTheFile {
	companion object {
		private val bytesWritten = ByteArray(512 * 1024)
		private val bytes = ByteArray(512 * 1024)
		private val bytesRead = ByteArray(512 * 1024)
		private var committedToCache = false

		@BeforeClass
		@JvmStatic
		fun context() {
			val deferredCommit = DeferredPromise(CachedFile())

			val fakeCacheStreamSupplier =
				object : SupplyCacheStreams {
					override fun promiseCachedFileOutputStream(uniqueKey: String): Promise<CacheOutputStream> {
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
			val diskFileCacheDataSource = DiskFileCacheDataSource(
				dataSource,
				fakeCacheStreamSupplier,
				mockk<CacheFiles>().apply {
					every { promiseCachedFile(any()) } returns Promise.empty()
				},
			)
			diskFileCacheDataSource.open(
				DataSpec.Builder()
					.setUri(Uri.parse("http://my-server/file"))
					.setPosition(0)
					.setLength((2 * 1024 * 1024).toLong()).setKey("1")
					.build()
			)
			do {
				val readResult = diskFileCacheDataSource.read(bytesRead, 0, bytesRead.size)
			} while (readResult != C.RESULT_END_OF_INPUT)
			diskFileCacheDataSource.close()

			try {
				deferredCommit.toExpiringFuture()[10, TimeUnit.SECONDS]
			} catch (e: TimeoutException) {
				// expected
			}
		}

		init {
			Random().nextBytes(bytes)
		}
	}

	@Test
	fun `then the file is not written`() {
		assertThat(bytesWritten).containsOnly(0)
	}

	@Test
	fun `then the file is not cached`() {
		assertThat(committedToCache).isFalse
	}

	@Test
	fun `then the file is read correctly`() {
		assertArrayEquals(bytes, bytesRead)
	}

}
