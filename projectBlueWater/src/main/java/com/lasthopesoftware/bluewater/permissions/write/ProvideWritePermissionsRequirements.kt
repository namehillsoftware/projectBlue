package com.lasthopesoftware.bluewater.permissions.write

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library

interface ProvideWritePermissionsRequirements {
    fun isWritePermissionsRequiredForLibrary(library: Library): Boolean
}
