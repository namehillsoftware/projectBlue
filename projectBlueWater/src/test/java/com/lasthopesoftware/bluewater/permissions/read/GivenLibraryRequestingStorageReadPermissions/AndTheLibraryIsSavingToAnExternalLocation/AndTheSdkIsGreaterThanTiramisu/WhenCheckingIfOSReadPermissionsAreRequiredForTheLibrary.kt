package com.lasthopesoftware.bluewater.permissions.read.GivenLibraryRequestingStorageReadPermissions.AndTheLibraryIsSavingToAnExternalLocation.AndTheSdkIsGreaterThanTiramisu

import android.os.Build
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
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
class WhenCheckingIfOSReadPermissionsAreRequiredForTheLibrary {
	private val isPermissionRequired by lazy {
		val applicationReadPermissionsRequirementsProvider = ApplicationReadPermissionsRequirementsProvider(
			mockk {
				every { isReadPermissionGranted } returns false
			}
		)

		applicationReadPermissionsRequirementsProvider
			.isReadPermissionsRequiredForLibrary(
				LibrarySettings(
					libraryId = LibraryId(707),
					connectionSettings = StoredMediaCenterConnectionSettings(
						syncedFileLocation = SyncedFileLocation.EXTERNAL,
					),
				)
			)
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(isPermissionRequired).isFalse
	}
}
