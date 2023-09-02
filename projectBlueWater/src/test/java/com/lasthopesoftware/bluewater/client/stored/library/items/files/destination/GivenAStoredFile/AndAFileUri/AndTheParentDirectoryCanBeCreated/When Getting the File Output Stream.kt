package com.lasthopesoftware.bluewater.client.stored.library.items.files.destination.GivenAStoredFile.AndAFileUri.AndTheParentDirectoryCanBeCreated

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.io.OsFileSupplier
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.io.OutputStream
import java.nio.file.Files

class `When Getting the File Output Stream` {

	private var outputStream: OutputStream? = null

	@BeforeAll
	fun before() {
		val tempPath = Files.createTempDirectory("5DdoL7j")
		val tempPathFile = tempPath.toFile()
		tempPathFile.deleteOnExit()
		val tempFile = File(File(tempPathFile, "that"), "knot")
		val storedFile = StoredFile(LibraryId(1), ServiceFile(1), tempFile.toURI(), true)
		storedFile.setIsDownloadComplete(true)
		val storedFileJobProcessor = StoredFileUriDestinationBuilder(
			OsFileSupplier,
			mockk {
				every { isFileWritePossible(any()) } returns true
			},
			mockk()
		)

		outputStream = storedFileJobProcessor.promiseOutputStream(storedFile).toExpiringFuture().get()
	}

	@Test
	fun `then the output stream is opened`() {
		assertThat(outputStream).isNotNull
	}
}
