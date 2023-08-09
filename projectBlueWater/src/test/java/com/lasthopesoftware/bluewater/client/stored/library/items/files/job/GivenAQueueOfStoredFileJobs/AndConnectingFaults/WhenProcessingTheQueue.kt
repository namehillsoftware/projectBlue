package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.AndConnectingFaults

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
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

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

	private val expectedStoredFiles = arrayOf(
		StoredFile().setServiceId(1).setLibraryId(1),
		StoredFile().setServiceId(4).setLibraryId(1),
		StoredFile().setServiceId(5).setLibraryId(1),
		StoredFile().setServiceId(7).setLibraryId(1),
		StoredFile().setServiceId(114).setLibraryId(1),
		StoredFile().setServiceId(92).setLibraryId(1)
	)
	private val storedFilesAccess = MarkedFilesStoredFileAccess()
	private lateinit var storedFileStatuses: List<StoredFileJobStatus>

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeAll
	fun act() {
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { getOutputStream(any()) } returns ByteArrayOutputStream()
			},
			storedFilesAccess,
			mockk {
				every { promiseDownload(any(), any()) } returns Promise(ByteArrayInputStream(ByteArray(0)))
				every { promiseDownload(any(), match { it.serviceId == 2 }) } returns Promise(IOException())
			},
			mockk { every { isFileReadPossible(any()) } returns true },
			mockk { every { isFileWritePossible(any()) } returns true },
			mockk(relaxed = true)
		)
		storedFileStatuses =
			storedFileJobProcessor.observeStoredFileDownload(storedFileJobs).toList().blockingGet()
	}

	@Test
	fun `then the error file is marked as queued`() {
		assertThat(storedFileStatuses.filter { s -> s.storedFile.serviceId == 2 }
			.map { r -> r.storedFileJobState }).containsExactly(
			StoredFileJobState.Queued,
			StoredFileJobState.Downloading,
			StoredFileJobState.Queued
		)
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
				.map { r -> r.storedFile }
		).containsExactlyElementsOf(storedFileJobs.map(StoredFileJob::storedFile))
	}

	@Test
	fun thenAllTheFilesAreBroadcastAsDownloaded() {
		assertThat(
			storedFileStatuses
				.filter { s -> s.storedFileJobState === StoredFileJobState.Downloaded }
				.map { r -> r.storedFile }
		).containsExactly(*expectedStoredFiles)
	}
}
