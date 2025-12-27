package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheDownloadFails

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.DeferredDownloadPromise
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.io.PromisingWritableStreamWrapper
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

class WhenProcessingTheJob {

	private var storedFileWriteException: Throwable? = null
	private val storedFile = StoredFile(LibraryId(5), ServiceFile("1"), URI("test-path"), true)
	private val states = ArrayList<StoredFileJobState>()

	@BeforeAll
	fun before() {
		val deferredDownload = DeferredDownloadPromise(byteArrayOf(
			(474 % 128).toByte(),
			(550 % 128).toByte(),
		))
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns PromisingWritableStreamWrapper(
					mockk(relaxUnitFun = true) { every { write(any(), any(), any()) } throws IOException() },
					null
				).toPromise()
			},
			mockk {
				every { promiseDownload(any(), any()) } returns deferredDownload
			},
			mockk(),
		)
		storedFileJobProcessor
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
			.blockingSubscribe(
				{ storedFileJobState -> states.add(storedFileJobState) }
			) { e -> storedFileWriteException = e }
	}

	@Test
	fun thenTheStoredFileIsPutBackIntoQueuedState() {
		assertThat(states).containsExactly(
			StoredFileJobState.Queued,
			StoredFileJobState.Downloading,
			StoredFileJobState.Queued
		)
	}

	@Test
	fun thenNoExceptionIsThrown() {
		assertThat(storedFileWriteException).isNull()
	}
}
