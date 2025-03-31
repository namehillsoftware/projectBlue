package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatExists.AndAnImpossibleFileRead

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
import org.junit.jupiter.api.Test
import java.net.URI

class WhenProcessingTheJob {
	private val jobStates by lazy {
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns Promise.empty()
			},
			mockk { every { promiseDownload(any(), any()) } returns Promise.empty() },
			mockk(),
		)
		storedFileJobProcessor
			.observeStoredFileDownload(
				setOf(
					StoredFileJob(
						LibraryId(12),
						ServiceFile("1"),
						StoredFile(LibraryId(12), ServiceFile("1"), URI("test-path"), true)
					)
				)
			)
			.map { j -> j.storedFileJobState }
			.toList()
			.blockingGet()
	}

	@Test
	fun `then the file state is unreadable`() {
		assertThat(jobStates).containsExactly(
			StoredFileJobState.Queued,
			StoredFileJobState.Unreadable
		)
	}
}
