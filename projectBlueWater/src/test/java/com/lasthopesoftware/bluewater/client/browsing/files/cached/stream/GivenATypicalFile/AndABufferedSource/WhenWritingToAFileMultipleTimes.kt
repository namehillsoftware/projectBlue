package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.GivenATypicalFile.AndABufferedSource

import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CachedFileOutputStream
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.mockk
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import java.util.*

class WhenWritingToAFileMultipleTimes {

	private val file by lazy {
		File.createTempFile("temp", ".txt").apply { deleteOnExit() }
	}
	private val bytes by lazy { ByteArray(2000000).also(Random()::nextBytes) }
	private val bytesWritten = ByteArray(2000000)
	private val firstBuffer by lazy { Buffer().write(bytes, 0, 1000000) }
	private val secondBuffer by lazy { Buffer().write(bytes, 1000000, 1000000) }

    @BeforeAll
    fun before() {
        val cachedFileOutputStream = CachedFileOutputStream(
            "unique-test", file, mockk()
        )
        cachedFileOutputStream.promiseTransfer(firstBuffer)
            .eventually { os -> os.promiseTransfer(secondBuffer) }
			.toExpiringFuture()
			.get()

		FileInputStream(file).use { fis -> fis.read(bytesWritten, 0, bytesWritten.size) }
    }

    @Test
    fun thenTheBytesAreWrittenCorrectly() {
        assertThat(bytesWritten).isEqualTo(bytes)
    }
}
