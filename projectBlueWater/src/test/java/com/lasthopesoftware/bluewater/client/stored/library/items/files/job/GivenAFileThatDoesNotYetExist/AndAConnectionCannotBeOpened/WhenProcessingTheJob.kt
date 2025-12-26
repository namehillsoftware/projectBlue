package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndAConnectionCannotBeOpened

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.NullPromisingWritableStream
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.io.PromisingReadableStream
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URI

class WhenProcessingTheJob {
    private val storedFile = StoredFile(LibraryId(4), ServiceFile("1"), URI("test://test-path"), true)
    private val jobStates by lazy {
		val deferredDownload = DeferredPromise<PromisingReadableStream>(IOException())
        val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } returns NullPromisingWritableStream.toPromise()
			},
			mockk { every { promiseDownload(any(), any()) } returns deferredDownload },
			mockk(),
		)
        storedFileJobProcessor
			.observeStoredFileDownload(
				Observable.just(
					StoredFileJob(
						LibraryId(4),
						ServiceFile("1"),
						storedFile
					)
				)
			)
            .map { s ->
				s.storedFileJobState.also {
					if (it == StoredFileJobState.Downloading)
						deferredDownload.resolve()
				}
			}
			.toList()
			.timeout(30, java.util.concurrent.TimeUnit.SECONDS)
			.blockingGet()
    }

    @Test
    fun `then the stored file job state is queued again`() {
        assertThat(jobStates).containsExactly(
            StoredFileJobState.Queued,
            StoredFileJobState.Downloading,
            StoredFileJobState.Queued
        )
    }
}
