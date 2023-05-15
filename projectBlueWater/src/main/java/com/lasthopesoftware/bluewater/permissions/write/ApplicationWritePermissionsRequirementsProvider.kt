package com.lasthopesoftware.bluewater.permissions.write

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.CheckLibraryStorageWritePermissionsRequirements
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.LibraryStorageWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker

class ApplicationWritePermissionsRequirementsProvider(
	private val storageWritePermissionsRequirementsProvider: CheckLibraryStorageWritePermissionsRequirements,
	private val checkOsPermissions: CheckOsPermissions
) : ProvideWritePermissionsRequirements {
    constructor(context: Context) : this(
        LibraryStorageWritePermissionsRequirementsProvider,
        OsPermissionsChecker(context)
    )

	override fun isWritePermissionsRequiredForLibrary(library: Library): Boolean {
        return storageWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(
            library
        ) && !checkOsPermissions.isWritePermissionGranted
    }
}
