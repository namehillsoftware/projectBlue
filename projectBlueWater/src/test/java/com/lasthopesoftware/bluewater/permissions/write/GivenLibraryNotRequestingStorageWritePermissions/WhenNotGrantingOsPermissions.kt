package com.lasthopesoftware.bluewater.permissions.write.GivenLibraryNotRequestingStorageWritePermissions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenNotGrantingOsPermissions {
	private val isPermissionGranted by lazy {
		val applicationWritePermissionsRequirementsProvider = ApplicationWritePermissionsRequirementsProvider(
			mockk {
				every { isWritePermissionsRequiredForLibrary(any()) } returns false
			},
			mockk {
				every { isWritePermissionGranted } returns false
			}
		)

		applicationWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(Library())
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(isPermissionGranted).isFalse
	}
}
