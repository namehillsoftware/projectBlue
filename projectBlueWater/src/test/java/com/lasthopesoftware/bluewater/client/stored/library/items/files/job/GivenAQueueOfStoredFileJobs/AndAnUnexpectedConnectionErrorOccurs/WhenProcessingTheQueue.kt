package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.AndAnUnexpectedConnectionErrorOccurs

import android.os.Build
import androidx.annotation.RequiresApi
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.MarkedFilesStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class WhenProcessingTheQueue {
	private val storedFileJobs: Set<StoredFileJob> = HashSet(
		listOf(
			StoredFileJob(
				LibraryId(1),
				ServiceFile(1),
				StoredFile().setServiceId(1).setLibraryId(1)
			),
			StoredFileJob(
				LibraryId(1),
				ServiceFile(2),
				StoredFile().setServiceId(2).setLibraryId(1)
			),
			StoredFileJob(
				LibraryId(1),
				ServiceFile(4),
				StoredFile().setServiceId(4).setLibraryId(1)
			),
			StoredFileJob(
				LibraryId(1),
				ServiceFile(5),
				StoredFile().setServiceId(5).setLibraryId(1)
			),
			StoredFileJob(
				LibraryId(1),
				ServiceFile(7),
				StoredFile().setServiceId(7).setLibraryId(1)
			),
			StoredFileJob(
				LibraryId(1),
				ServiceFile(114),
				StoredFile().setServiceId(114).setLibraryId(1)
			),
			StoredFileJob(
				LibraryId(1),
				ServiceFile(92),
				StoredFile().setServiceId(92).setLibraryId(1)
			)
		)
	)
	private val expectedStoredFiles = arrayOf<StoredFile>(
		StoredFile().setServiceId(1).setLibraryId(1),
		StoredFile().setServiceId(2).setLibraryId(1)
	)
	private val storedFilesAccess = MarkedFilesStoredFileAccess()
	private val storedFileStatuses = ArrayList<StoredFileJobStatus>()
	private var exception: StoredFileJobException? = null

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeAll
	fun act() {
		val storedFileJobProcessor = StoredFileJobProcessor(
			{ storedFile ->
				mockk {
					every { parentFile } returns null
					every { exists() } returns storedFile.isDownloadComplete
					if (storedFile.serviceId == 4) {
						every { path } returns "write-failure"
					}
				}
			},
			storedFilesAccess,
			{ _, f ->
				if (f.serviceId != 4) Promise(ByteArrayInputStream(ByteArray(0)))
				else Promise(UnexpectedException())
			},
			{ true },
			{ true },
			mockk(relaxed = true))

		storedFileJobProcessor.observeStoredFileDownload(storedFileJobs).blockingSubscribe(
			storedFileStatuses::add
		) { e: Throwable? ->
			if (e is StoredFileJobException) exception = e
		}
	}

    @Test
    fun `then the error file is correct`() {
        assertThat(exception?.storedFile?.serviceId).isEqualTo(4)
    }

    @Test
    fun `then the unexpected exception is correct`() {
        assertThat(exception?.cause).isInstanceOf(UnexpectedException::class.java)
    }

    @Test
    fun `then the files are marked as downloaded`() {
        assertThat(storedFilesAccess.storedFilesMarkedAsDownloaded)
            .containsExactly(*expectedStoredFiles)
    }

    @Test
    fun `then the files are broadcast as downloading`() {
        assertThat(
            storedFileStatuses
                .filter { s -> s.storedFileJobState === StoredFileJobState.Downloading }
                .map { r -> r.storedFile.serviceId }
        ).containsOnly(1, 2, 4)
    }

    @Test
    fun `then all the files are broadcast as downloaded`() {
        assertThat(
            storedFileStatuses
                .filter { s -> s.storedFileJobState === StoredFileJobState.Downloaded }
                .map { r -> r.storedFile }
        ).containsExactly(*expectedStoredFiles)
    }

	private class UnexpectedException : RuntimeException()
}
