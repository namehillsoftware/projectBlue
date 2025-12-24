package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterAResponseIsReceived

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.DeferredDownloadPromise
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.NullPromisingWritableStream
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI

class WhenProcessingTheJob {
    private val storedFile = StoredFile(LibraryId(15), ServiceFile("1"), URI("test-path"), true)
	private val updateStoredFiles = mockk<UpdateStoredFiles> {
		every { markStoredFileAsDownloaded(any()) } answers { Promise(firstArg<StoredFile>().setIsDownloadComplete(true)) }
	}
    private var states: List<StoredFileJobState>? = null

	@BeforeAll
    fun before() {
		val deferredDownloadPromise = DeferredDownloadPromise(byteArrayOf(120, (573 % 128).toByte()))
        val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns NullPromisingWritableStream.toPromise()
			},
			mockk {
				every { promiseDownload(any(), any()) } returns deferredDownloadPromise
			},
			updateStoredFiles,
		)
        states = storedFileJobProcessor
			.observeStoredFileDownload(
				Observable.just(
					StoredFileJob(
						LibraryId(15),
						ServiceFile("1"),
						storedFile
					)
				)
			)
            .map { f ->
				f.storedFileJobState.also {
					if (it == StoredFileJobState.Downloading)
						deferredDownloadPromise.resolve()
				}
			}
            .toList().blockingGet()
    }

    @Test
    fun `then the file is marked as downloaded`() {
		verify(exactly = 1) { updateStoredFiles.markStoredFileAsDownloaded(storedFile) }
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
