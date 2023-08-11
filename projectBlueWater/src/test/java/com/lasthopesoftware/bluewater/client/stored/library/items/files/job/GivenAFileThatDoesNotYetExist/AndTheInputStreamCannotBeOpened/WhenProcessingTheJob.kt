package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheInputStreamCannotBeOpened

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI

class WhenProcessingTheJob {
	private val storedFile = StoredFile(LibraryId(6), ServiceFile(1), URI("test-path"), true)
	private var jobStates: List<StoredFileJobState>? = null

	@BeforeAll
	fun before() {
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { getOutputStream(any()) } returns ByteArrayOutputStream()
			},
			mockk(relaxed = true),
			mockk { every { promiseDownload(any(), any()) } returns Promise(IOException()) }
		)

		jobStates = storedFileJobProcessor.observeStoredFileDownload(
			setOf(
				StoredFileJob(LibraryId(6), ServiceFile(1), storedFile)
			)
		).map { j -> j.storedFileJobState }.toList().blockingGet()
	}

	@Test
	fun `then the stored file job is in queued state`() {
		assertThat(jobStates).containsExactly(
			StoredFileJobState.Queued,
			StoredFileJobState.Downloading,
			StoredFileJobState.Queued
		)
	}
}
