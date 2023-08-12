package com.lasthopesoftware.bluewater.client.stored.library.items.files.destination.GivenAStoredFile.AndTheParentDirectoryCannotBeCreated

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ExecutionException

class `When Getting the File Output Stream` {

	private var storageCreatePathException: StorageCreatePathException? = null

	@BeforeAll
	fun before() {
		val tempPath = Files.createTempDirectory("DFK5eGq")
		val tempPathFile = tempPath.toFile()
		tempPathFile.deleteOnExit()
		tempPathFile.setReadOnly()
		val tempFile = File(File(tempPathFile, "Poe53ykw"), "17jnCe0")
		val storedFile = StoredFile(LibraryId(1), ServiceFile(1), tempFile.toURI(), true)
		storedFile.setIsDownloadComplete(true)
		val storedFileJobProcessor = StoredFileUriDestinationBuilder(mockk())

		try {
			storedFileJobProcessor.promiseOutputStream(storedFile).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			storageCreatePathException = e.cause as? StorageCreatePathException
		}
	}

	@Test
	fun `then a storage create path exception is thrown`() {
		assertThat(storageCreatePathException).isNotNull
	}
}
