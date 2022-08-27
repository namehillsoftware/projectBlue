package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAFileThatDoesNotYetExist.WithParentDirectory.ThatCannotBeCreated

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
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

class WhenProcessingTheJob {
    private val storedFile = StoredFile(LibraryId(7), 1, ServiceFile(1), "test-path", true)
    private var storageCreatePathException: StorageCreatePathException? = null
    @BeforeAll
    fun before() {
        val storedFileJobProcessor = StoredFileJobProcessor(
            {
				mockk {
					every { exists() } returns false
					every { parentFile } returns mockk {
						every { exists() } returns false
						every { mkdirs() } returns false
					}
				}
            },
            mockk(),
            { _, _ -> Promise(ByteArrayInputStream(ByteArray(0))) },
            { false },
            { true },
            mockk(relaxUnitFun = true))
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
