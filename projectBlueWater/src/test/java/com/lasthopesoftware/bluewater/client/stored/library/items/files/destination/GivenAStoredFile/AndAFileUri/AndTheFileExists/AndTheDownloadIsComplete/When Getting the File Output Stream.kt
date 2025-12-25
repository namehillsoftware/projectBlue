package com.lasthopesoftware.bluewater.client.stored.library.items.files.destination.GivenAStoredFile.AndAFileUri.AndTheFileExists.AndTheDownloadIsComplete

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.io.OsFileSupplier
import com.lasthopesoftware.resources.io.PromisingWritableStream
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class `When Getting the File Output Stream` {

	private var outputStream: PromisingWritableStream? = null

	@BeforeAll
	fun before() {
		val tempFile = File.createTempFile("80l7u", "r24gdQ1c")
		tempFile.deleteOnExit()
		val storedFile = StoredFile(LibraryId(1), ServiceFile("1"), tempFile.toURI(), true)
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
	fun `then the output stream is correct`() {
		assertThat(outputStream).isNull()
	}
}
