package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.AndAnUnexpectedTransferErrorOccurs

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.DeferredDownloadPromise
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs.MarkedFilesStoredFilesUpdater
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.NullPromisingWritableStream
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.io.PromisingWritableStreamWrapper
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.function.Function

class WhenProcessingTheQueue {
	private val storedFileJobs: Set<StoredFileJob> = setOf(
		StoredFileJob(
			LibraryId(1),
			ServiceFile("1"),
			StoredFile().setServiceId("1").setLibraryId(1)
		),
		StoredFileJob(
			LibraryId(1),
			ServiceFile("2"),
			StoredFile().setServiceId("2").setLibraryId(1)
		),
		StoredFileJob(
			LibraryId(1),
			ServiceFile("4"),
			StoredFile().setServiceId("4").setLibraryId(1)
		),
		StoredFileJob(
			LibraryId(1),
			ServiceFile("5"),
			StoredFile().setServiceId("5").setLibraryId(1)
		),
		StoredFileJob(
			LibraryId(1),
			ServiceFile("7"),
			StoredFile().setServiceId("7").setLibraryId(1)
		),
		StoredFileJob(
			LibraryId(1),
			ServiceFile("114"),
			StoredFile().setServiceId("114").setLibraryId(1)
		),
		StoredFileJob(
			LibraryId(1),
			ServiceFile("92"),
			StoredFile().setServiceId("92").setLibraryId(1)
		)
	)

	private val expectedStoredFiles = arrayOf(
		StoredFile().setServiceId("1").setLibraryId(1),
		StoredFile().setServiceId("2").setLibraryId(1),
		StoredFile().setServiceId("4").setLibraryId(1)
	)
	private val storedFilesUpdater = MarkedFilesStoredFilesUpdater()
	private val storedFileStatuses = ArrayList<StoredFileJobStatus>()
	private var exception: StoredFileJobException? = null

	@BeforeAll
	fun before() {
		val deferredDownloadProvider = Function<StoredFile, DeferredDownloadPromise> {
			DeferredDownloadPromise(byteArrayOf(5, 10))
		}

		val storedFileDownloadMap = mutableMapOf<StoredFile, DeferredDownloadPromise>()
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns NullPromisingWritableStream.toPromise()
				every { promiseOutputStream(match { it.serviceId == "5" }) } returns Promise(
					PromisingWritableStreamWrapper(
						mockk(relaxUnitFun = true) { every { write(any(), any(), any()) } throws UnexpectedException() }
					)
				)
			},
			mockk {
				every { promiseDownload(any(), any()) } answers {
					val storedFile = secondArg<StoredFile>()
					storedFileDownloadMap.computeIfAbsent(storedFile, deferredDownloadProvider)
				}
			},
			storedFilesUpdater,
		)

		storedFileJobProcessor
			.observeStoredFileDownload(Observable.fromIterable(storedFileJobs))
			.doOnEach { n ->
				val (storedFile, status) = n.value ?: return@doOnEach
				if (status == StoredFileJobState.Downloading)
					storedFileDownloadMap.computeIfAbsent(storedFile, deferredDownloadProvider).resolve()
			}
			.timeout(30, TimeUnit.SECONDS)
			.blockingSubscribe(storedFileStatuses::add) { e -> if (e is StoredFileJobException) exception = e }
	}

	@Test
	fun `then the error file is correct`() {
		assertThat(exception?.storedFile?.serviceId).isEqualTo("5")
	}

	@Test
	fun `then the unexpected exception is correct`() {
		assertThat(exception?.cause).isInstanceOf(UnexpectedException::class.java)
	}

	@Test
	fun `then the files are marked as downloaded`() {
		assertThat(storedFilesUpdater.storedFilesMarkedAsDownloaded)
			.containsExactly(*expectedStoredFiles)
	}

	@Test
	fun `then the files are broadcast as downloading`() {
		assertThat(
			storedFileStatuses
				.filter { s -> s.storedFileJobState === StoredFileJobState.Downloading }
				.map { r -> r.storedFile.serviceId }
		).containsExactly("1", "2", "4", "5")
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
