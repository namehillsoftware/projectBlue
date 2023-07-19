package com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.cache.GivenAServiceFile.AndTheCachedFileDoesExistOnDisk.AndItIsEmpty

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.DiskCacheImageAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

private const val libraryId = 787
private const val serviceFileId = 899

class `When Getting the Image Bytes` {

	private val expectedBytes = byteArrayOf(
		(892 % 128).toByte(),
		(283 % 128).toByte(),
		(492 % 128).toByte(),
	)

	private val file = File("65vrgZkd57")

	private val mut by lazy {
        DiskCacheImageAccess(
            mockk {
                every {
                    promiseImageBytes(
                        LibraryId(libraryId),
                        ServiceFile(serviceFileId)
                    )
                } returns Promise(expectedBytes)
            },
            mockk {
                every {
                    promiseImageCacheKey(
                        LibraryId(libraryId),
                        ServiceFile(serviceFileId)
                    )
                } returns Promise("pvamAttX")
            },
            mockk {
                every {
                    promiseCachedFile(
                        LibraryId(libraryId),
                        "pvamAttX"
                    )
                } returns Promise(file)
                every { put(LibraryId(libraryId), "pvamAttX", any()) } answers {
                    storedBytes = lastArg()
                    Promise.empty()
                }
            }
        )
	}

	private var storedBytes: ByteArray? = null
	private var returnedBytes: ByteArray? = null

	@BeforeAll
	fun act() {
		try {
			file.createNewFile()

			returnedBytes = mut.promiseImageBytes(LibraryId(libraryId), ServiceFile(serviceFileId)).toExpiringFuture().get()
		} finally {
			file.delete()
		}
	}

	@Test
	fun `then the result is correct`() {
		assertThat(returnedBytes).isEqualTo(expectedBytes)
	}

	@Test
	fun `then the stored bytes are correct`() {
		assertThat(storedBytes).isEqualTo(expectedBytes)
	}
}
