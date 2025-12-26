package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.DeferredDownloadPromise
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.io.PromisingWritableStreamWrapper
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit

class WhenProcessingTheJob {
	private val storedFile = StoredFile(LibraryId(5), ServiceFile("1"), URI("test-path"), true)
	private val storedFileAccess = mockk<UpdateStoredFiles> {
		every { markStoredFileAsDownloaded(any()) } answers { Promise(firstArg<StoredFile>().setIsDownloadComplete(true)) }
	}
	private var states: List<StoredFileJobState>? = null
	private var downloadedBytes = emptyByteArray

	@BeforeAll
	fun before() {
		val os = ByteArrayOutputStream()
		val deferredDownload = DeferredDownloadPromise(byteArrayOf(907.toByte(), 403.toByte()))
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns PromisingWritableStreamWrapper(os).toPromise()
			},
			mockk {
				every { promiseDownload(any(), any()) } returns deferredDownload
			},
			storedFileAccess,
		)
		states = storedFileJobProcessor
			.observeStoredFileDownload(
				Observable.just(
					StoredFileJob(
						LibraryId(5),
						ServiceFile("1"),
						storedFile
					)
				)
			)
			.map { f ->
				f.storedFileJobState.also {
					if (it == StoredFileJobState.Downloading)
						deferredDownload.resolve()
				}
			}
			.timeout(30, TimeUnit.SECONDS)
			.toList()
			.blockingGet()
		downloadedBytes = os.toByteArray()
	}

	@Test
	fun `then the downloaded bytes are correct`() {
		assertThat(downloadedBytes).isEqualTo(byteArrayOf(907.toByte(), 403.toByte()))
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
