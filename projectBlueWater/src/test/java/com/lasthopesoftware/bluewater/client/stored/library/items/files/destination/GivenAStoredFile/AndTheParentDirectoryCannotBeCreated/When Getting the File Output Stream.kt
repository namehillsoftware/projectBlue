package com.lasthopesoftware.bluewater.client.stored.library.items.files.destination.GivenAStoredFile.AndTheParentDirectoryCannotBeCreated

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class `When Getting the File Output Stream` {

	private var storageCreatePathException: StorageCreatePathException? = null

	@BeforeAll
	fun before() {
		val tempPath = Files.createTempDirectory("DFK5eGq")
		val tempPathFile = tempPath.toFile()
		tempPathFile.deleteOnExit()
		tempPathFile.setReadOnly()
		val tempFile = File(File(tempPathFile, "Poe53ykw"), "17jnCe0")
		val storedFile = StoredFile(LibraryId(1), 1, ServiceFile(1), tempFile.path, true)
		storedFile.setIsDownloadComplete(true)
		val storedFileJobProcessor = StoredFileUriDestinationBuilder(mockk())

		try {
			storedFileJobProcessor.getOutputStream(storedFile)
		} catch (e: StorageCreatePathException) {
			storageCreatePathException = e
		}
	}

	@Test
	fun `then a storage create path exception is thrown`() {
		assertThat(storageCreatePathException).isNotNull
	}
}
