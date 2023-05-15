package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library

object LibraryStorageReadPermissionsRequirementsProvider :
    CheckLibraryStorageReadPermissionRequirements {
    override fun isReadPermissionsRequiredForLibrary(library: Library): Boolean {
        return library.isUsingExistingFiles || Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(
            library.syncedFileLocation
        )
    }
}
