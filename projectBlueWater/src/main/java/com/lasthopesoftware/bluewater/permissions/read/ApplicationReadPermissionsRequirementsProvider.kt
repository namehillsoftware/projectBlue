package com.lasthopesoftware.bluewater.permissions.read

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.isReadPermissionsRequiredForLibrary
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker

class ApplicationReadPermissionsRequirementsProvider(
	private val storageReadPermissionArbitratorForOs: CheckOsPermissions
) : ProvideReadPermissionsRequirements {
    constructor(context: Context) : this(OsPermissionsChecker(context))

	override fun isReadPermissionsRequiredForLibrary(library: Library): Boolean {
        return library.isReadPermissionsRequiredForLibrary
			&& !storageReadPermissionArbitratorForOs.isReadPermissionGranted
    }

	override fun isReadMediaPermissionsRequiredForLibrary(library: Library): Boolean {
		return library.isReadPermissionsRequiredForLibrary
			&& !storageReadPermissionArbitratorForOs.isReadMediaAudioPermissionGranted
	}
}
