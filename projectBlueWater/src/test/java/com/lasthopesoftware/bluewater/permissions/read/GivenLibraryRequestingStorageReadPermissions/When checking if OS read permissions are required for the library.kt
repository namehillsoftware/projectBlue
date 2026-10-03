package com.lasthopesoftware.bluewater.permissions.read.GivenLibraryRequestingStorageReadPermissions

import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.permissions.read.RequiredLibraryPermissionsProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When checking if OS read permissions are required for the library` {
	private val isPermissionRequired by lazy {
		val applicationReadPermissionsRequirementsProvider = RequiredLibraryPermissionsProvider(
			mockk {
				every { isReadPermissionGranted } returns true
			}
		)

		applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(LibrarySettings())
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(isPermissionRequired).isFalse
	}
}
