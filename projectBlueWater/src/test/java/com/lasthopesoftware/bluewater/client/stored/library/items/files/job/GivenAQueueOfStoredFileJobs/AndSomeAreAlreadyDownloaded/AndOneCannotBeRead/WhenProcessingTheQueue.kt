package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.AndSomeAreAlreadyDownloaded.AndOneCannotBeRead

import android.os.Build
import androidx.annotation.RequiresApi
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.MarkedFilesStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.storage.read.exceptions.StorageReadFileException
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class WhenProcessingTheQueue {
	private val storedFileJobs = setOf(
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
			StoredFile().setServiceId(5).setLibraryId(1).setIsDownloadComplete(true)
		),
		StoredFileJob(
			LibraryId(1),
			ServiceFile(7),
			StoredFile().setServiceId(7).setLibraryId(1)
		),
		StoredFileJob(
			LibraryId(1),
			ServiceFile(114),
			StoredFile().setServiceId(114).setLibraryId(1).setIsDownloadComplete(true)
		),
		StoredFileJob(
			LibraryId(1),
			ServiceFile(92),
			StoredFile().setServiceId(92).setLibraryId(1).setIsDownloadComplete(true)
		)
	)

	private val expectedStoredFiles = arrayOf(
		StoredFile().setServiceId(1).setLibraryId(1),
		StoredFile().setServiceId(2).setLibraryId(1),
		StoredFile().setServiceId(4).setLibraryId(1),
		StoredFile().setServiceId(7).setLibraryId(1)
	)
	private val storedFilesAccess = MarkedFilesStoredFileAccess()
	private val storedFileStatuses = ArrayList<StoredFileJobStatus>()
	private var exception: StorageReadFileException? = null

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeAll
	fun before() {
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { getFile(any()) } answers {
					val storedFile = firstArg<StoredFile>()
					mockk {
						every { parentFile } returns null
						every { exists() } returns storedFile.isDownloadComplete
						every { path } returns if (storedFile.serviceId == 114) "unreadable" else ""
					}
				}
			},
			storedFilesAccess,
			mockk {
				every { promiseDownload(any(), any()) } returns Promise(ByteArrayInputStream(ByteArray(0)))
			},
			mockk {
				every { isFileReadPossible(any()) } returns true
				every { isFileReadPossible(match { it.path == "unreadable" }) } returns false
		  	},
			mockk { every { isFileWritePossible(any()) } returns true },
			mockk(relaxUnitFun = true)
		)
		storedFileJobProcessor
			.observeStoredFileDownload(storedFileJobs)
			.blockingSubscribe(storedFileStatuses::add)
			{ error -> if (error is StorageReadFileException) exception = error }
	}

	@Test
	fun `then no error occurs`() {
		assertThat(exception).isNull()
	}

	@Test
	fun `then the correct files are marked unreadable`() {
		assertThat(
			storedFileStatuses.single { s -> s.storedFileJobState === StoredFileJobState.Unreadable }.storedFile.serviceId
		).isEqualTo(114)
	}

	@Test
	fun `then the correct files are marked as downloaded`() {
		assertThat(storedFilesAccess.storedFilesMarkedAsDownloaded)
			.containsExactly(*expectedStoredFiles)
	}

	@Test
	fun `then the files are broadcast as downloading`() {
		assertThat(
			storedFileStatuses
				.filter { s -> s.storedFileJobState === StoredFileJobState.Downloading }
				.map { r -> r.storedFile }
		).containsExactly(*expectedStoredFiles)
	}

	@Test
	fun `then the correct files are broadcast as downloaded`() {
		assertThat(
			storedFileStatuses
				.filter { s -> s.storedFileJobState === StoredFileJobState.Downloaded }
				.map { r -> r.storedFile }
		).isSubsetOf(
			storedFileJobs
				.map(StoredFileJob::storedFile)
				.filter { f -> f.serviceId != 114 }
		)
	}
}
