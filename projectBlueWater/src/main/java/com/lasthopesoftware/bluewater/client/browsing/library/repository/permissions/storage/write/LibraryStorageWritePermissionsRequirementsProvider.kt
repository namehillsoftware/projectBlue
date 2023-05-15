package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library

object LibraryStorageWritePermissionsRequirementsProvider :
    CheckLibraryStorageWritePermissionsRequirements {
    override fun isWritePermissionsRequiredForLibrary(library: Library): Boolean {
        return Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.syncedFileLocation)
    }
}
