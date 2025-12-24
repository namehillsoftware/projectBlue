package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndNoBytesAreDownloaded

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.io.PromisingReadableStreamWrapper
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.net.URI

class WhenProcessingTheJob {

	private val storedFile = StoredFile(LibraryId(5), ServiceFile("1"), URI("test-path"), true)
	private val states = ArrayList<StoredFileJobState>()

	@BeforeAll
	fun before() {
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns Promise(OutputStream.nullOutputStream())
			},
			mockk {
				every { promiseDownload(any(), any()) } returns Promise(
					PromisingReadableStreamWrapper(ByteArrayInputStream(emptyByteArray))
				)
			},
			mockk {
				every { markStoredFileAsDownloaded(storedFile) } returns storedFile.toPromise()
			},
		)
		storedFileJobProcessor.observeStoredFileDownload(
			Observable.just(
				StoredFileJob(
					LibraryId(5),
					ServiceFile("1"),
					storedFile
				)
			)
		)
			.map { f -> f.storedFileJobState }
			.blockingSubscribe(
				{ storedFileJobState -> states.add(storedFileJobState) },
			)
	}

	@Test
	fun thenTheStoredFileIsPutBackIntoQueuedState() {
		assertThat(states).containsExactly(
			StoredFileJobState.Queued,
			StoredFileJobState.Downloading,
			StoredFileJobState.Queued
		)
	}
}
