package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterItIsDownloaded

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class WhenProcessingTheJob {
	private val storedFile = StoredFile(LibraryId(13), 1, ServiceFile(1), "test-path", true)
	private val storedFileAccess = mockk<AccessStoredFiles> {
		every { markStoredFileAsDownloaded(any()) } answers { Promise(firstArg<StoredFile>()) }
	}
	private val states: MutableList<StoredFileJobState> = ArrayList()

	@BeforeAll
	fun before() {
		val fakeConnectionProvider = FakeConnectionProvider()
		fakeConnectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				200,
				ByteArray(0)
			)
		})
		val storedFileJobProcessor = StoredFileJobProcessor(
			{
				mockk {
					every { parentFile } returns null
					every { exists() } returns false
				}
			},
			storedFileAccess,
			{ _, _ ->
				Promise(
					ByteArrayInputStream(
						ByteArray(0)
					)
				)
			},
			{ false },
			{ true },
			mockk(relaxUnitFun = true))
		storedFileJobProcessor
			.observeStoredFileDownload(
				setOf(
					StoredFileJob(
						LibraryId(13),
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
					if (status.storedFileJobState == StoredFileJobState.Downloaded) disposable.dispose()
				}

				override fun onError(e: Throwable) {}
				override fun onComplete() {}
			})
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
