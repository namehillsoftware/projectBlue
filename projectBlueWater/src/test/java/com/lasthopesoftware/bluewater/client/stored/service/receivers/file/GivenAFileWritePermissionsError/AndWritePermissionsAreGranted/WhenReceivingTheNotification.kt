package com.lasthopesoftware.bluewater.client.stored.service.receivers.file.GivenAFileWritePermissionsError.AndWritePermissionsAreGranted

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileWritePermissionsReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.LinkedList

class WhenReceivingTheNotification {
	private val requestedWritePermissionLibraries: MutableList<LibraryId> = LinkedList()

	@BeforeAll
	fun before() {
		val storedFileAccess = mockk<AccessStoredFiles>().apply {
			every { promiseStoredFile(14) } returns Promise(StoredFile().setId(14).setLibraryId(22))
		}

		val storedFileWritePermissionsReceiver = StoredFileWritePermissionsReceiver(
			mockk {
				every { isWritePermissionGranted } returns true
			},
			{ e -> requestedWritePermissionLibraries.add(e) },
			storedFileAccess
		)
		ExpiringFuturePromise(storedFileWritePermissionsReceiver.receive(14)).get()
	}

	@Test
	fun `then no read permissions requests are sent`() {
		assertThat(requestedWritePermissionLibraries).isEmpty()
	}
}
