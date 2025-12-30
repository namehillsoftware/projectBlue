package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.WithDuplicates

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.DeferredDownloadPromise
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.MarkedFilesStoredFilesUpdater
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.NullPromisingWritableStream
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Arrays
import java.util.concurrent.TimeUnit
import java.util.function.Function

class WhenProcessingTheQueue {
	private val storedFileJobs: Iterable<StoredFileJob> = Arrays.asList(
		StoredFileJob(LibraryId(1), ServiceFile("1"), StoredFile().setServiceId("1").setLibraryId(1)),
		StoredFileJob(LibraryId(1), ServiceFile("2"), StoredFile().setServiceId("2").setLibraryId(1)),
		StoredFileJob(LibraryId(1), ServiceFile("4"), StoredFile().setServiceId("4").setLibraryId(1)),
		StoredFileJob(LibraryId(1), ServiceFile("5"), StoredFile().setServiceId("5").setLibraryId(1)),
		StoredFileJob(LibraryId(1), ServiceFile("7"), StoredFile().setServiceId("7").setLibraryId(1)),
		StoredFileJob(
			LibraryId(1),
			ServiceFile("114"),
			StoredFile().setServiceId("114").setLibraryId(1)
		),
		StoredFileJob(
			LibraryId(1),
			ServiceFile("114"),
			StoredFile().setServiceId("114").setLibraryId(1)
		),
		StoredFileJob(LibraryId(1), ServiceFile("92"), StoredFile().setServiceId("92").setLibraryId(1)),
		StoredFileJob(LibraryId(1), ServiceFile("5"), StoredFile().setServiceId("5").setLibraryId(1))
	)
	private val expectedStoredFiles = arrayOf(
		StoredFile().setServiceId("1").setLibraryId(1),
		StoredFile().setServiceId("2").setLibraryId(1),
		StoredFile().setServiceId("4").setLibraryId(1),
		StoredFile().setServiceId("5").setLibraryId(1),
		StoredFile().setServiceId("7").setLibraryId(1),
		StoredFile().setServiceId("114").setLibraryId(1),
		StoredFile().setServiceId("92").setLibraryId(1)
	)
	private val storedFilesUpdater = MarkedFilesStoredFilesUpdater()
	private lateinit var storedFileStatuses: List<StoredFileJobStatus>

	@BeforeAll
	fun before() {
		val deferredDownloadProvider = Function<StoredFile, DeferredDownloadPromise> {
			DeferredDownloadPromise(byteArrayOf(310.toByte(), 153.toByte(), 120.toByte()))
		}
		val storedFileDownloadMap = mutableMapOf<StoredFile, DeferredDownloadPromise>()
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns NullPromisingWritableStream.toPromise()
			},
			mockk {
				every { promiseDownload(any(), any()) } answers {
					val storedFile = secondArg<StoredFile>()
					storedFileDownloadMap.computeIfAbsent(storedFile, deferredDownloadProvider)
				}
			},
			storedFilesUpdater,
		)
		storedFileStatuses =
			storedFileJobProcessor
				.observeStoredFileDownload(Observable.fromIterable(storedFileJobs))
				.doOnEach { n ->
					val (storedFile, status) = n.value ?: return@doOnEach
					if (status == StoredFileJobState.Downloading)
						storedFileDownloadMap.computeIfAbsent(storedFile, deferredDownloadProvider).resolve()
				}
				.timeout(30, TimeUnit.SECONDS)
				.toList()
				.blockingGet()
	}

	@Test
	fun `then the files are all marked as downloaded`() {
		assertThat(storedFilesUpdater.storedFilesMarkedAsDownloaded)
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
	fun `then all the files are broadcast as downloaded`() {
		assertThat(
			storedFileStatuses
				.filter { s -> s.storedFileJobState === StoredFileJobState.Downloaded }
				.map { r -> r.storedFile }
		).containsOnly(*expectedStoredFiles)
	}
}
