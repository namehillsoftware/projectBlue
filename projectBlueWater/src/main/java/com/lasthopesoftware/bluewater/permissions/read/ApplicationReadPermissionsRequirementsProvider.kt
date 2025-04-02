package com.lasthopesoftware.bluewater.permissions.read

import android.os.Build
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.isReadPermissionsRequired
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions

class ApplicationReadPermissionsRequirementsProvider(
	private val storageReadPermissionArbitratorForOs: CheckOsPermissions
) : ProvideReadPermissionsRequirements {

	override fun isReadPermissionsRequiredForLibrary(librarySettings: LibrarySettings): Boolean =
		Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
			&& librarySettings.isReadPermissionsRequired
			&& !storageReadPermissionArbitratorForOs.isReadPermissionGranted

	override fun isReadMediaPermissionsRequiredForLibrary(library: LibrarySettings): Boolean =
		library.isReadPermissionsRequired
			&& !storageReadPermissionArbitratorForOs.isReadMediaAudioPermissionGranted

}
