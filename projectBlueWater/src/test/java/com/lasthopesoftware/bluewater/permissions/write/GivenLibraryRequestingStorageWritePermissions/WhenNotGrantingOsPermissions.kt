package com.lasthopesoftware.bluewater.permissions.write.GivenLibraryRequestingStorageWritePermissions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenNotGrantingOsPermissions {
	private val isPermissionGranted by lazy {
		val applicationWritePermissionsRequirementsProvider = ApplicationWritePermissionsRequirementsProvider(
			{ true }
		) { false }

		applicationWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(Library())
	}

	@Test
	fun `then permissions are required`() {
		assertThat(isPermissionGranted).isTrue
	}
}
