package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheDownloadFails

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.resources.io.WriteFileStreams;
import com.namehillsoftware.handoff.promises.Promise;

import org.assertj.core.api.Assertions.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import io.mockk.every;
import io.mockk.mockk;

class WhenProcessingTheJob {

	companion object {
		private var storedFileWriteException: Throwable? = null
		private val storedFile = StoredFile(LibraryId(5), 1, ServiceFile(1), "test-path", true)
		private val states: MutableList<StoredFileJobState> = ArrayList()

		@RequiresApi(api = Build.VERSION_CODES.N)
		@JvmStatic
		@BeforeClass
		fun before() {
			val storedFileJobProcessor = StoredFileJobProcessor(
				{
					val file = mockk<File>(relaxed = true)
					val parentFile = mockk<File>(relaxed = true).apply {
						every { exists() } returns false
						every { mkdirs() } returns true
					}
					every { file.parentFile } returns parentFile
					file
				},
				mockk(),
				{ _, _ -> Promise(ByteArrayInputStream(ByteArray(0))) },
				{ false },
				{ true },
				mockk<WriteFileStreams>().apply { every { writeStreamToFile(any(), any()) } throws IOException() })
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
