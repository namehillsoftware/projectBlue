package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterAResponseIsReceived

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class WhenProcessingTheJob {
    private val storedFile = StoredFile(LibraryId(15), 1, ServiceFile(1), "test-path", true)
	private val storedFileAccess = mockk<AccessStoredFiles> {
		every { markStoredFileAsDownloaded(any()) } answers { Promise(firstArg<StoredFile>()) }
	}
    private var states: List<StoredFileJobState>? = null

	@BeforeAll
    fun before() {
        val fakeConnectionProvider = FakeConnectionProvider()
        fakeConnectionProvider.mapResponse({
            FakeConnectionResponseTuple(
                200,
                ByteArray(0)
            )
        })
        val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { getOutputStream(any()) } returns ByteArrayOutputStream()
			},
            storedFileAccess,
			mockk { every { promiseDownload(any(), any()) } returns Promise(ByteArrayInputStream(ByteArray(0))) },
			mockk { every { isFileReadPossible(any()) } returns false },
			mockk { every { isFileWritePossible(any()) } returns true },
            mockk(relaxUnitFun = true))
        states = storedFileJobProcessor.observeStoredFileDownload(
            setOf(
                StoredFileJob(
                    LibraryId(15),
                    ServiceFile(1),
                    storedFile
                )
            )
        )
            .map { f -> f.storedFileJobState }
            .toList().blockingGet()
    }

    @Test
    fun `then the file is marked as downloaded`() {
		verify(exactly = 1) { storedFileAccess.markStoredFileAsDownloaded(storedFile) }
    }

    @Test
    fun `then the job states progress correctly`() {
        assertThat(states).containsExactly(
            StoredFileJobState.Queued,
            StoredFileJobState.Downloading,
            StoredFileJobState.Downloaded
        )
    }
}
