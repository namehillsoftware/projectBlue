package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.AndTheParentDirectoryCannotBeCreated

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI

class WhenProcessingTheJob {
    private val storedFile = StoredFile(LibraryId(7), ServiceFile(1), URI("test-path"), true)
    private var storageCreatePathException: StorageCreatePathException? = null

    @BeforeAll
    fun before() {
        val storedFileJobProcessor = StoredFileJobProcessor(
			mockk {
				every { promiseOutputStream(any()) } throws StorageCreatePathException(File("JN7DGC9O"))
			},
            mockk(),
			mockk { every { promiseDownload(any(), any()) } returns Promise(ByteArrayInputStream(ByteArray(0))) }
		)
        try {
            storedFileJobProcessor.observeStoredFileDownload(
                setOf(
                    StoredFileJob(LibraryId(7), ServiceFile(1), storedFile)
                )
            ).blockingSubscribe()
        } catch (e: Throwable) {
            storageCreatePathException = e.cause as? StorageCreatePathException ?: throw e
        }
    }

    @Test
    fun `then a storage create path exception is thrown`() {
        assertThat(storageCreatePathException).isNotNull
    }
}
