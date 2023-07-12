package com.lasthopesoftware.bluewater.client.playback.caching.GivenAFileLessThan1Megabyte.AndItHasASetEndLength

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.EntireFileCachedDataSource
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class WhenStreamingTheFile {
	companion object {
		private val bytes by lazy { ByteArray(512 * 1024).also { Random().nextBytes(it) } }
		private val bytesRead = ByteArray(512 * 1024)

		@BeforeClass
		@JvmStatic
		fun context() {
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
				LibraryId(286),
                dataSource,
                mockk(), // Use a strict mock to ensure the cache is not opened
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
		}
	}

	@Test
	fun `then the file is read correctly`() {
		assertArrayEquals(bytes, bytesRead)
	}
}
