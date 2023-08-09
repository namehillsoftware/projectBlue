package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndAConnectionCannotBeOpened

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException

class WhenProcessingTheJob {
    private val storedFile = StoredFile(LibraryId(4), 1, ServiceFile(1), "test-path", true)
    private val jobStates by lazy {
        val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { getOutputStream(any()) } returns ByteArrayOutputStream()
			},
            mockk(),
            mockk { every { promiseDownload(any(), any()) } returns Promise(IOException()) },
            mockk { every { isFileReadPossible(any()) } returns false },
			mockk { every { isFileWritePossible(any()) } returns true },
            mockk(relaxUnitFun = true))
        storedFileJobProcessor
			.observeStoredFileDownload(
				setOf(
					StoredFileJob(
						LibraryId(4),
						ServiceFile(1),
						storedFile
					)
				)
			)
            .map { s: StoredFileJobStatus -> s.storedFileJobState }
            .toList()
            .blockingGet()
    }

    @Test
    fun `then the stored file job state is queued again`() {
        assertThat(jobStates).containsExactly(
            StoredFileJobState.Queued,
            StoredFileJobState.Downloading,
            StoredFileJobState.Queued
        )
    }
}
