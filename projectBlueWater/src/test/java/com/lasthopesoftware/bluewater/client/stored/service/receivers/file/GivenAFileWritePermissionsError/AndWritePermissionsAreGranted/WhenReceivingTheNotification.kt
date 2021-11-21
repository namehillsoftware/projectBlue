package com.lasthopesoftware.bluewater.client.stored.service.receivers.file.GivenAFileWritePermissionsError.AndWritePermissionsAreGranted

import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileWritePermissionsReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class WhenReceivingTheNotification {
    @Test
    fun thenNoReadPermissionsRequestsAreSent() {
        Assertions.assertThat(requestedWritePermissionLibraries).isEmpty()
    }

    companion object {
        private val requestedWritePermissionLibraries: MutableList<Int> = LinkedList()
        @BeforeClass
        @Throws(Exception::class)
        fun before() {
            val storedFileAccess = Mockito.mock(
                AccessStoredFiles::class.java
            )
            Mockito.`when`(storedFileAccess.getStoredFile(14))
                .thenReturn(Promise(StoredFile().setId(14).setLibraryId(22)))
            val storedFileWritePermissionsReceiver = StoredFileWritePermissionsReceiver(
                { true }, { e: Int -> requestedWritePermissionLibraries.add(e) },
                storedFileAccess
            )
            FuturePromise(storedFileWritePermissionsReceiver.receive(14)).get()
        }
    }
}
