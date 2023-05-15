package com.lasthopesoftware.bluewater.client.stored.service.receivers.file.GivenAFileReadPermissionsError.AndReadPermissionsAreNotGranted

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileReadPermissionsReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.LinkedList

class WhenReceivingTheNotification {
	private val requestedReadPermissionLibraries: MutableList<LibraryId> = LinkedList()

	@BeforeAll
	fun act() {
		val storedFileAccess = mockk<AccessStoredFiles>().apply {
			every { getStoredFile(14) } returns Promise(StoredFile().setId(14).setLibraryId(22))
		}

		val storageReadPermissionsRequestedBroadcaster = StoredFileReadPermissionsReceiver(
			mockk {
				every { isReadPermissionGranted } returns false
			},
			{ e -> requestedReadPermissionLibraries.add(e) },
			storedFileAccess
		)
		ExpiringFuturePromise(storageReadPermissionsRequestedBroadcaster.receive(14)).get()
	}

	@Test
	fun `then a read permissions request is sent`() {
		assertThat(requestedReadPermissionLibraries).containsOnly(LibraryId(22))
	}
}
