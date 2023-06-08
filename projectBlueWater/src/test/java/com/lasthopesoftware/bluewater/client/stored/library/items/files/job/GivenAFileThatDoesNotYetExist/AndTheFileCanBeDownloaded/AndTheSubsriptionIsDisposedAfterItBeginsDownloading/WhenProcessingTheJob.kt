package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterItBeginsDownloading

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class WhenProcessingTheJob {
	private val storedFile = StoredFile(LibraryId(55), 1, ServiceFile(1), "test-path", true)
	private val storedFileAccess = mockk<AccessStoredFiles>()
	private val states: MutableList<StoredFileJobState> = ArrayList()

	@BeforeAll
	fun before() {
		val deferredPromise = DeferredPromise<InputStream>(ByteArrayInputStream(ByteArray(0)))
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { getFile(any()) } returns mockk {
					every { parentFile } returns null
					every { exists() } returns false
				}
			},
			storedFileAccess,
			mockk { every { promiseDownload(any(), any()) } returns deferredPromise },
			mockk { every { isFileReadPossible(any()) } returns false },
			mockk { every { isFileWritePossible(any()) } returns true },
			mockk(relaxUnitFun = true)
		)
		storedFileJobProcessor.observeStoredFileDownload(
			setOf(
				StoredFileJob(
					LibraryId(55),
					ServiceFile(1),
					storedFile
				)
			)
		)
			.blockingSubscribe(object : Observer<StoredFileJobStatus> {
				private lateinit var disposable: Disposable

				override fun onSubscribe(d: Disposable) {
					disposable = d
				}

				override fun onNext(status: StoredFileJobStatus) {
					states.add(status.storedFileJobState)
					if (status.storedFileJobState != StoredFileJobState.Downloading) return
					disposable.dispose()
					deferredPromise.resolve()
				}

				override fun onError(e: Throwable) {}
				override fun onComplete() {}
			})
	}

	@Test
	fun `then the file is not marked as downloaded`() {
		verify(exactly = 0) { storedFileAccess.markStoredFileAsDownloaded(storedFile) }
	}

	@Test
	fun `then the job states progress correctly`() {
		assertThat(states).containsExactly(StoredFileJobState.Queued, StoredFileJobState.Downloading)
	}
}
