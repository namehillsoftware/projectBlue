package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library

interface CheckLibraryStorageWritePermissionsRequirements {
    fun isWritePermissionsRequiredForLibrary(library: Library): Boolean
}
