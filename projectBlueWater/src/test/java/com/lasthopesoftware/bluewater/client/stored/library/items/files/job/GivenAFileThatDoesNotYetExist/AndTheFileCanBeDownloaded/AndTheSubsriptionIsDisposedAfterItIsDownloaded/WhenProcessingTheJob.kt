package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterItIsDownloaded

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.DeferredDownloadPromise
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.NullPromisingWritableStream
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.concurrent.TimeUnit

class WhenProcessingTheJob {
	private val storedFile = StoredFile(LibraryId(13), ServiceFile("1"), URI("test-path"), true)
	private val updateStoredFiles = mockk<UpdateStoredFiles> {
		every { markStoredFileAsDownloaded(any()) } answers { Promise(firstArg<StoredFile>().setIsDownloadComplete(true)) }
	}
	private val states: MutableList<StoredFileJobState> = ArrayList()

	@OptIn(ExperimentalStdlibApi::class)
	@BeforeAll
	fun before() {
		val storedFileDownloadMap = mutableMapOf<StoredFile, DeferredDownloadPromise>()
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns NullPromisingWritableStream.toPromise()
			},
			mockk {
				every { promiseDownload(any(), any()) } answers {
					val storedFile = secondArg<StoredFile>()
					storedFileDownloadMap
						.computeIfAbsent(storedFile) { DeferredDownloadPromise("590d88fbd4994ee0a7d03ed155e6c969".hexToByteArray()) }
				}
			},
			updateStoredFiles,
		)
		storedFileJobProcessor
			.observeStoredFileDownload(
				Observable.just(
					StoredFileJob(
						LibraryId(13),
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
					when (status.storedFileJobState) {
						StoredFileJobState.Downloading -> storedFileDownloadMap[status.storedFile]?.resolve()
						StoredFileJobState.Downloaded -> disposable.dispose()
						else -> {}
					}
				}

				override fun onError(e: Throwable) {}
				override fun onComplete() {}
			})
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
