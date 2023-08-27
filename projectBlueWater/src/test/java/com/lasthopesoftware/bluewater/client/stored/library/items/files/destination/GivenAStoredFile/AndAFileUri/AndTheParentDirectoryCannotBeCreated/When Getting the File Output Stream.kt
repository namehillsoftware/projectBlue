package com.lasthopesoftware.bluewater.client.stored.library.items.files.destination.GivenAStoredFile.AndAFileUri.AndTheParentDirectoryCannotBeCreated

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class `When Getting the File Output Stream` {

	private var storageCreatePathException: StorageCreatePathException? = null

	@BeforeAll
	fun before() {
		val tempPath = Files.createTempDirectory("DFK5eGq")
		val tempPathFile = tempPath.toFile().apply { deleteOnExit() }
		val tempFile = ParentProxyFile(ParentProxyFile(tempPathFile, "Poe53ykw"), "17jnCe0")

		val tempFileUri = tempFile.toURI()

		val storedFileJobProcessor = StoredFileUriDestinationBuilder(
			mockk {
				every { getFile(tempFileUri) } returns tempFile
			},
			mockk(),
			mockk()
		)

		try {
			val storedFile = StoredFile(LibraryId(1), ServiceFile(1), tempFileUri, true)
			storedFile.setIsDownloadComplete(true)
			storedFileJobProcessor.promiseOutputStream(storedFile).toExpiringFuture().get(1, TimeUnit.MINUTES)
		} catch (e: ExecutionException) {
			storageCreatePathException = e.cause as? StorageCreatePathException ?: throw e
		}
	}

	@Test
	fun `then a storage create path exception is thrown`() {
		assertThat(storageCreatePathException).isNotNull
	}

	private class ParentProxyFile(private val parent: File, child: String) : File(parent, child) {
		override fun getParentFile(): File = parent

		override fun exists(): Boolean = false

		override fun mkdirs(): Boolean = false
	}
}
