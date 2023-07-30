package com.lasthopesoftware.bluewater.permissions.read.GivenLibraryRequestingStorageReadPermissions.AndTheLibraryIsSavingToAnExternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When checking if OS read permissions are required for the library` {
	private val isPermissionRequired by lazy {
		val applicationReadPermissionsRequirementsProvider = ApplicationReadPermissionsRequirementsProvider(
			mockk {
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
