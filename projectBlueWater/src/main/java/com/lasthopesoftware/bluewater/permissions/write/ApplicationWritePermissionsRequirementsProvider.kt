package com.lasthopesoftware.bluewater.permissions.write

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.isWritePermissionsRequiredForLibrary
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker

class ApplicationWritePermissionsRequirementsProvider(
	private val checkOsPermissions: CheckOsPermissions
) : ProvideWritePermissionsRequirements {
    constructor(context: Context) : this(OsPermissionsChecker(context))

	override fun isWritePermissionsRequiredForLibrary(library: Library): Boolean {
        return library.isWritePermissionsRequiredForLibrary && !checkOsPermissions.isWritePermissionGranted
    }
}
