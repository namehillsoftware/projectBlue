package com.lasthopesoftware.bluewater.client.stored.service.receivers.file.GivenAFileWritePermissionsError.AndWritePermissionsAreGranted

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileWritePermissionsReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class WhenReceivingTheNotification {
    @Test
    fun thenNoReadPermissionsRequestsAreSent() {
        Assertions.assertThat(requestedWritePermissionLibraries).isEmpty()
    }

    companion object {
        private val requestedWritePermissionLibraries: MutableList<LibraryId> = LinkedList()

        @BeforeClass
		@JvmStatic
        fun before() {
            val storedFileAccess = mockk<AccessStoredFiles>().apply {
            	every { getStoredFile(14) } returns Promise(StoredFile().setId(14).setLibraryId(22))
			}

            val storedFileWritePermissionsReceiver = StoredFileWritePermissionsReceiver(
                { true },
				{ e -> requestedWritePermissionLibraries.add(e) },
                storedFileAccess
            )
            FuturePromise(storedFileWritePermissionsReceiver.receive(14)).get()
        }
    }
}
