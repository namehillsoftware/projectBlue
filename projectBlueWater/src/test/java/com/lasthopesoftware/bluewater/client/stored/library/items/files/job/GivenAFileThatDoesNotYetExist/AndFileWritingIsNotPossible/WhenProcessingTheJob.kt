package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndFileWritingIsNotPossible

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenProcessingTheJob {
	private var storedFileWriteException: StoredFileWriteException? = null

	@BeforeAll
	fun before() {
		val storedFile = StoredFile(LibraryId(1), 1, ServiceFile(1), "test-path", true)
		storedFile.setIsDownloadComplete(true)
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { getFile(any()) } returns mockk {
					every { parentFile } returns null
					every { exists() } returns false
				}
			},
			mockk(),
			mockk { every { promiseDownload(any(), any()) } returns Promise.empty() },
			mockk { every { isFileReadPossible(any()) } returns false },
			mockk { every { isFileWritePossible(any()) } returns false },
			mockk(relaxUnitFun = true)
		)

		try {
			storedFileJobProcessor.observeStoredFileDownload(
				setOf(
					StoredFileJob(LibraryId(1), ServiceFile(1), storedFile)
				)
			).blockingSubscribe()
		} catch (e: Throwable) {
			storedFileWriteException = e.cause as? StoredFileWriteException ?: throw e
		}
	}

	@Test
	fun `then a stored file write exception is thrown`() {
		assertThat(storedFileWriteException).isNotNull
	}
}
