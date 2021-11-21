package com.lasthopesoftware.bluewater.client.stored.service.receivers.file.GivenAFileReadPermissionsError.AndReadPermissionsAreGranted

import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileReadPermissionsReceiver
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
        Assertions.assertThat(requestedReadPermissionLibraries).isEmpty()
    }

    companion object {
        private val requestedReadPermissionLibraries: MutableList<Int> = LinkedList()
        @BeforeClass
        @Throws(Exception::class)
        fun before() {
            val storedFileAccess = Mockito.mock(
                AccessStoredFiles::class.java
            )
            Mockito.`when`(storedFileAccess.getStoredFile(14))
                .thenReturn(Promise(StoredFile().setId(14).setLibraryId(22)))
            val storageReadPermissionsRequestedBroadcaster = StoredFileReadPermissionsReceiver(
                { true }, { e: Int -> requestedReadPermissionLibraries.add(e) },
                storedFileAccess
            )
            FuturePromise(storageReadPermissionsRequestedBroadcaster.receive(14)).get()
        }
    }
}
