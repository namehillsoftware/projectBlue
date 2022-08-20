package com.lasthopesoftware.bluewater.permissions.read.GivenLibraryNotRequestingStorageReadPermissions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenNotGrantingOsPermissions {
	private val isPermissionRequired by lazy {
		val applicationReadPermissionsRequirementsProvider = ApplicationReadPermissionsRequirementsProvider(
			{ false }
		) { false }

		applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(Library())
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(isPermissionRequired).isFalse
	}
}
