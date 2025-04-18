package com.lasthopesoftware.bluewater.permissions.read.GivenLibraryNotRequestingStorageReadPermissions

import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
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

		applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(LibrarySettings())
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(isPermissionRequired).isFalse
	}
}
