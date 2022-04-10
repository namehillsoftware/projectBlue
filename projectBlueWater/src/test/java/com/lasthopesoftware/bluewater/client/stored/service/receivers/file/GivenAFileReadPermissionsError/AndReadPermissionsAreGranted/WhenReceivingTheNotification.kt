package com.lasthopesoftware.bluewater.client.stored.service.receivers.file.GivenAFileReadPermissionsError.AndReadPermissionsAreGranted

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileReadPermissionsReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
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
		Assertions.assertThat(requestedReadPermissionLibraries).isEmpty()
	}

	companion object {
		private val requestedReadPermissionLibraries: MutableList<LibraryId> = LinkedList()

		@BeforeClass
		@JvmStatic
		fun before() {
			val storedFileAccess = mockk<AccessStoredFiles>().apply {
				every { getStoredFile(14) } returns Promise(StoredFile().setId(14).setLibraryId(22))
			}

			val storageReadPermissionsRequestedBroadcaster = StoredFileReadPermissionsReceiver(
				{ true },
				{ e -> requestedReadPermissionLibraries.add(e) },
				storedFileAccess
			)
			ExpiringFuturePromise(storageReadPermissionsRequestedBroadcaster.receive(14)).get()
		}
	}
}
