package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterItBeginsDownloading

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.DeferredDownloadPromise
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.io.PromisingWritableStreamWrapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit

class WhenProcessingTheJob {
	private val storedFile = StoredFile(LibraryId(55), ServiceFile("1"), URI("test://test-path"), true)
	private val storedFileUpdater = mockk<UpdateStoredFiles>()
	private val states: MutableList<StoredFileJobState> = ArrayList()
	private var downloadedBytes = emptyByteArray

	@BeforeAll
	fun before() {
		val os = ByteArrayOutputStream()
		val deferredPromise = DeferredDownloadPromise(byteArrayOf(498.toByte(), 416.toByte()))
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns PromisingWritableStreamWrapper(os, null).toPromise()
			},
			mockk { every { promiseDownload(any(), any()) } returns deferredPromise },
			storedFileUpdater,
		)
		storedFileJobProcessor
			.observeStoredFileDownload(
				Observable.just(
					StoredFileJob(
						LibraryId(55),
						ServiceFile("1"),
						storedFile
					)
				)
			)
			.timeout(30, TimeUnit.SECONDS)
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

		downloadedBytes = os.toByteArray()
	}

	@Test
	fun `then the downloaded bytes are correct`() {
		assertThat(downloadedBytes).isEmpty()
	}

	@Test
	fun `then the file is not marked as downloaded`() {
		verify(exactly = 0) { storedFileUpdater.markStoredFileAsDownloaded(storedFile) }
	}

	@Test
	fun `then the job states progress correctly`() {
		assertThat(states).containsExactly(StoredFileJobState.Queued, StoredFileJobState.Downloading)
	}
}
