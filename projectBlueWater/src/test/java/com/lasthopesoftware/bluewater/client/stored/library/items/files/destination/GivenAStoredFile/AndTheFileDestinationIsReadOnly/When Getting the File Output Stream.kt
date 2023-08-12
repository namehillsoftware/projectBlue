package com.lasthopesoftware.bluewater.client.stored.library.items.files.destination.GivenAStoredFile.AndTheFileDestinationIsReadOnly

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.ExecutionException

class `When Getting the File Output Stream` {

	private var storedFileWriteException: StoredFileWriteException? = null

	@BeforeAll
	fun before() {
		val tempFile = File.createTempFile("test-path", "tst")
		tempFile.deleteOnExit()
		tempFile.setReadOnly()
		val storedFile = StoredFile(LibraryId(1), ServiceFile(1), tempFile.toURI(), true)
		storedFile.setIsDownloadComplete(true)
		val storedFileJobProcessor = StoredFileUriDestinationBuilder(mockk())

		try {
			storedFileJobProcessor.promiseOutputStream(storedFile).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			storedFileWriteException = e.cause as? StoredFileWriteException
		}
	}

	@Test
	fun `then a stored file write exception is thrown`() {
		assertThat(storedFileWriteException).isNotNull
	}
}
