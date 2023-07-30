package com.lasthopesoftware.bluewater.permissions.read

import android.os.Build
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.isReadPermissionsRequiredForLibrary
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions

class ApplicationReadPermissionsRequirementsProvider(
	private val storageReadPermissionArbitratorForOs: CheckOsPermissions
) : ProvideReadPermissionsRequirements {

	override fun isReadPermissionsRequiredForLibrary(library: Library): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
			&& library.isReadPermissionsRequiredForLibrary
			&& !storageReadPermissionArbitratorForOs.isReadPermissionGranted
    }

	override fun isReadMediaPermissionsRequiredForLibrary(library: Library): Boolean {
		return library.isReadPermissionsRequiredForLibrary
			&& !storageReadPermissionArbitratorForOs.isReadMediaAudioPermissionGranted
	}
}
