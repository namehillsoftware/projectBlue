package com.lasthopesoftware.bluewater.client.playback.caching.GivenARemoteFileWithUnsetLength

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.EntireFileCachedDataSource
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
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
		private const val fileSize = 512 * 1024
		private val bytesWritten = ByteArray(fileSize)
		private val bytes = ByteArray(fileSize)
		private val bytesRead = ByteArray(fileSize)
		private var committedToCache = false

		@BeforeClass
		@JvmStatic
		fun context() {
			val deferredCommit = DeferredPromise(CachedFile())

			val buffer = Buffer()
			buffer.write(bytes)
			val dataSource = mockk<HttpDataSource>(relaxUnitFun = true).apply {
				every { open(any()) } returns C.LENGTH_UNSET.toLong()

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
                dataSource,
                mockk(), // Use a strict stream supplier mock to ensure a cache is never opened
			)
			diskFileCacheDataSource.open(
				DataSpec.Builder()
					.setUri(Uri.parse("http://my-server/file"))
					.setPosition(0)
					.setLength(C.LENGTH_UNSET.toLong()).setKey("1")
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