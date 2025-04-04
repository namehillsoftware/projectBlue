package com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.cache.GivenAServiceFile.AndTheCachedFileDoesNotExistOnDisk.AndTheSourceImageIsEmpty

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

class `When Getting the Image Bytes` {

	companion object {
		private const val libraryId = 754
		private const val serviceFileId = "818"
	}

	private val expectedBytes = byteArrayOf()

	private val mut by lazy {
		DiskCacheImageAccess(
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(expectedBytes)
			},
			mockk {
				every { promiseImageCacheKey(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise("pvamAttX")
			},
			mockk {
				every { promiseCachedFile(LibraryId(libraryId), "pvamAttX") } returns Promise(File("y1GUF"))
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
		returnedBytes = mut.promiseImageBytes(LibraryId(libraryId), ServiceFile(serviceFileId)).toExpiringFuture().get()
	}

	@Test
	fun `then the result is correct`() {
		assertThat(returnedBytes).isEqualTo(expectedBytes)
	}

	@Test
	fun `then there are no stored bytes because no data was returned from the server`() {
		assertThat(storedBytes).isNull()
	}
}
