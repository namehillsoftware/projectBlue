package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library

interface CheckLibraryStorageReadPermissionRequirements {
    fun isReadPermissionsRequiredForLibrary(library: Library): Boolean
}
