package com.lasthopesoftware.bluewater.permissions.read.GivenLibraryNotRequestingStorageReadPermissions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGrantingOsPermissions {
	private val isPermissionGranted by lazy {
		val applicationReadPermissionsRequirementsProvider = ApplicationReadPermissionsRequirementsProvider(
			{ false }
		) { true }

		applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(Library())
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(isPermissionGranted).isFalse
	}
}
