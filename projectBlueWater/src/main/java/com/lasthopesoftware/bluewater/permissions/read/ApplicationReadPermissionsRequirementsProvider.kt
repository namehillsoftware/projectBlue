package com.lasthopesoftware.bluewater.permissions.read

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.ILibraryStorageReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.LibraryStorageReadPermissionsRequirementsProvider
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs

class ApplicationReadPermissionsRequirementsProvider(
    private val libraryStorageReadPermissionsRequirementsProvider: ILibraryStorageReadPermissionsRequirementsProvider,
    private val storageReadPermissionArbitratorForOs: IStorageReadPermissionArbitratorForOs
) : IApplicationReadPermissionsRequirementsProvider {
    constructor(context: Context) : this(
        LibraryStorageReadPermissionsRequirementsProvider(),
        ExternalStorageReadPermissionsArbitratorForOs(context)
    ) {
    }

    override fun isReadPermissionsRequiredForLibrary(library: Library): Boolean {
        return libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(
            library
        ) && !storageReadPermissionArbitratorForOs.isReadPermissionGranted
    }
}
