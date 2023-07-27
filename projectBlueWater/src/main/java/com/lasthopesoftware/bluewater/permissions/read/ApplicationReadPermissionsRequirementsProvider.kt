package com.lasthopesoftware.bluewater.permissions.read

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.CheckLibraryStorageReadPermissionRequirements
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.LibraryStorageReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.OsPermissionsChecker

class ApplicationReadPermissionsRequirementsProvider(
	private val libraryStorageReadPermissionsRequirementsProvider: CheckLibraryStorageReadPermissionRequirements,
	private val storageReadPermissionArbitratorForOs: CheckOsPermissions
) : ProvideReadPermissionsRequirements {
    constructor(context: Context) : this(
        LibraryStorageReadPermissionsRequirementsProvider,
        OsPermissionsChecker(context)
    )

	override fun isReadPermissionsRequiredForLibrary(library: Library): Boolean {
        return libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(library)
			&& !storageReadPermissionArbitratorForOs.isReadPermissionGranted
    }

	override fun isReadMediaPermissionsRequiredForLibrary(library: Library): Boolean {
		return libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(library)
			&& !storageReadPermissionArbitratorForOs.isReadMediaAudioPermissionGranted
	}
}
