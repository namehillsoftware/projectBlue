package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.GivenATypicalFile.AndABufferedSource

import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CachedFileWritableStream
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.io.BufferedSourcePromisingStream
import io.mockk.mockk
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import java.util.Random

class WhenWritingToAFileMultipleTimes {

	private val file by lazy {
		File.createTempFile("temp", ".txt").apply { deleteOnExit() }
	}
	private val bytes by lazy { ByteArray(2000000).also(Random()::nextBytes) }
	private val bytesWritten = ByteArray(2000000)
	private val firstBuffer by lazy { BufferedSourcePromisingStream(Buffer().write(bytes, 0, 1000000)) }
	private val secondBuffer by lazy { BufferedSourcePromisingStream(Buffer().write(bytes, 1000000, 1000000)) }

    @BeforeAll
    fun before() {
        val cachedFileOutputStream = CachedFileWritableStream(
            LibraryId(500),"unique-test", file, mockk()
        )

        cachedFileOutputStream.promiseCopyFrom(firstBuffer)
            .eventually { cachedFileOutputStream.promiseCopyFrom(secondBuffer) }
			.toExpiringFuture()
			.get()

		FileInputStream(file).use { fis -> fis.read(bytesWritten, 0, bytesWritten.size) }
    }

    @Test
    fun thenTheBytesAreWrittenCorrectly() {
        assertThat(bytesWritten).isEqualTo(bytes)
    }
}
