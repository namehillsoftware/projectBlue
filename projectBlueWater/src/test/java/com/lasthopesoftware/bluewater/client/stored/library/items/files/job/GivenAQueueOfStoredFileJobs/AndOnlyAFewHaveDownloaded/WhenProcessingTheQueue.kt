package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.AndOnlyAFewHaveDownloaded

import android.os.Build
import androidx.test.filters.SdkSuppress
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeJRiverConnectionProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.MarkedFilesStoredFilesUpdater
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class WhenProcessingTheQueue {
	private val storedFileJobs: Set<StoredFileJob> = setOf(
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
	private val expectedDownloadingStoredFiles = arrayOf(
		StoredFile().setServiceId(1).setLibraryId(1),
		StoredFile().setServiceId(2).setLibraryId(1),
		StoredFile().setServiceId(4).setLibraryId(1),
		StoredFile().setServiceId(5).setLibraryId(1),
		StoredFile().setServiceId(7).setLibraryId(1)
	)
	private val expectedDownloadedStoredFiles = arrayOf(
		StoredFile().setServiceId(1).setLibraryId(1),
		StoredFile().setServiceId(2).setLibraryId(1),
		StoredFile().setServiceId(4).setLibraryId(1),
		StoredFile().setServiceId(5).setLibraryId(1)
	)
	private val storedFilesUpdater = MarkedFilesStoredFilesUpdater()
	private var storedFileStatuses: List<StoredFileJobStatus> = ArrayList()

	@BeforeAll
	@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
	fun before() {
		val fakeConnectionProvider = spyk<FakeJRiverConnectionProvider>()
		fakeConnectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				200,
				ByteArray(0)
			)
		})
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns ByteArrayOutputStream().toPromise()
			},
			mockk {
				every { promiseDownload(any(), any()) } answers {
					val f = lastArg<StoredFile>()
					if (expectedDownloadedStoredFiles.contains(f)) Promise(ByteArrayInputStream(ByteArray(0)))
					else DeferredPromise(ByteArrayInputStream(ByteArray(0)))
				}
			},
			storedFilesUpdater,
		)
		storedFileStatuses = storedFileJobProcessor
			.observeStoredFileDownload(storedFileJobs)
			.timeout(1, TimeUnit.SECONDS) { it.onComplete() }
			.toList().blockingGet()
	}

	@Test
	fun `then the files are broadcast as queued`() {
		assertThat(
			storedFileStatuses
				.filter { s -> s.storedFileJobState === StoredFileJobState.Queued }
				.map { r -> r.storedFile }
		).isSubsetOf(storedFileJobs.map(StoredFileJob::storedFile))
	}

	@Test
	fun `then the correct files are broadcast as downloading`() {
		assertThat(
			storedFileStatuses
				.filter { s -> s.storedFileJobState === StoredFileJobState.Downloading }
				.map { r -> r.storedFile }
		).containsOnly(*expectedDownloadingStoredFiles)
	}

	@Test
	fun `then the correct files are broadcast as downloaded`() {
		assertThat(
			storedFileStatuses
				.filter { s -> s.storedFileJobState === StoredFileJobState.Downloaded }
				.map { r -> r.storedFile }
		).containsOnly(*expectedDownloadedStoredFiles)
	}
}
