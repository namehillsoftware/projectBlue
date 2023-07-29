package com.lasthopesoftware.bluewater.permissions.read.GivenLibraryRequestingStorageReadPermissions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenNotGrantingOsPermissions {
	private val isPermissionRequired by lazy {
		val applicationReadPermissionsRequirementsProvider = ApplicationReadPermissionsRequirementsProvider(
            mockk<CheckOsPermissions> {
				every { isReadPermissionGranted } returns false
			}
		)

		applicationReadPermissionsRequirementsProvider
			.isReadPermissionsRequiredForLibrary(
				Library().setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)
			)
	}

	@Test
	fun `then permissions are required`() {
		assertThat(isPermissionRequired).isTrue
	}
}
