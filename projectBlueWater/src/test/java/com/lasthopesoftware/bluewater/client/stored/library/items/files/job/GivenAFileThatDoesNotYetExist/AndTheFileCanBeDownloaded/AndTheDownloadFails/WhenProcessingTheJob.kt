package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheDownloadFails

import android.os.Build
import androidx.annotation.RequiresApi
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class WhenProcessingTheJob {

	private var storedFileWriteException: Throwable? = null
	private val storedFile = StoredFile(LibraryId(5), 1, ServiceFile(1), "test-path", true)
	private val states = ArrayList<StoredFileJobState>()

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeAll
	fun before() {
		val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { getOutputStream(any()) } returns mockk(relaxUnitFun = true) {
					every { write(any(), any(), any()) } throws IOException()
				}
			},
			mockk(),
			mockk {
				every { promiseDownload(any(), any()) } returns Promise(
					ByteArrayInputStream(
						byteArrayOf(
							(474 % 128).toByte(),
							(550 % 128).toByte(),
						)
					)
				)
			},
			mockk { every { isFileReadPossible(any()) } returns false },
			mockk { every { isFileWritePossible(any()) } returns true },
			mockk { every { writeStreamToFile(any(), any()) } throws IOException() })
		storedFileJobProcessor.observeStoredFileDownload(
			setOf(
				StoredFileJob(
					LibraryId(5),
					ServiceFile(1),
					storedFile
				)
			)
		)
			.map { f -> f.storedFileJobState }
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
