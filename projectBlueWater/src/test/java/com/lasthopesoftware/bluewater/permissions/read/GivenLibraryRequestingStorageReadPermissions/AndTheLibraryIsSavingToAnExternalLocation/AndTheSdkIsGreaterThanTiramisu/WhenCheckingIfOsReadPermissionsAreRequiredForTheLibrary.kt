package com.lasthopesoftware.bluewater.permissions.read.GivenLibraryRequestingStorageReadPermissions.AndTheLibraryIsSavingToAnExternalLocation.AndTheSdkIsGreaterThanTiramisu

import android.os.Build
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class WhenCheckingIfOsReadPermissionsAreRequiredForTheLibrary {
	private val isPermissionRequired by lazy {
		val applicationReadPermissionsRequirementsProvider = ApplicationReadPermissionsRequirementsProvider(
			mockk {
				every { isReadPermissionGranted } returns false
			}
		)

		applicationReadPermissionsRequirementsProvider
			.isReadPermissionsRequiredForLibrary(
				Library(syncedFileLocation = Library.SyncedFileLocation.EXTERNAL)
			)
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(isPermissionRequired).isFalse
	}
}
