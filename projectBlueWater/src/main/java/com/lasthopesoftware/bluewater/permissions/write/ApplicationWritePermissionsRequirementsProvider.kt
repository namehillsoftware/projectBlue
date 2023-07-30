package com.lasthopesoftware.bluewater.permissions.write

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.isWritePermissionsRequiredForLibrary
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions

class ApplicationWritePermissionsRequirementsProvider(
	private val checkOsPermissions: CheckOsPermissions
) : ProvideWritePermissionsRequirements {

	override fun isWritePermissionsRequiredForLibrary(library: Library): Boolean {
        return library.isWritePermissionsRequiredForLibrary && !checkOsPermissions.isWritePermissionGranted
    }
}
