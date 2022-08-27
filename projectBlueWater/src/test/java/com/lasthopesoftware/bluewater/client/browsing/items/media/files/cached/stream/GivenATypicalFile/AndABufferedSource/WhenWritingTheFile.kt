package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.GivenATypicalFile.AndABufferedSource

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CachedFileOutputStream
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.mockk
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import java.util.*

class WhenWritingTheFile {
	private val file by lazy {
		File.createTempFile("deafen", ".tmp").apply { deleteOnExit() }
	}
	private val bytes by lazy { ByteArray(2000000).also(Random()::nextBytes) }
	private val bytesWritten = ByteArray(2000000)

    @BeforeAll
    fun before() {
        val cachedFileOutputStream = CachedFileOutputStream("unique-test", file, mockk())
        val buffer = Buffer()
        buffer.write(bytes)
        cachedFileOutputStream.promiseTransfer(buffer).toExpiringFuture().get()

		FileInputStream(file).use { fis ->
			fis.read(bytesWritten, 0, bytes.size)
		}
    }

    @Test
    fun thenTheBytesAreWrittenCorrectly() {
        assertThat(bytesWritten).isEqualTo(bytes)
    }
}
