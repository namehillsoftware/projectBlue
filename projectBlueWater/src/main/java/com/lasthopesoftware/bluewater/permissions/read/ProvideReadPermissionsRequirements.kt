package com.lasthopesoftware.bluewater.permissions.read

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library

interface ProvideReadPermissionsRequirements {
    fun isReadPermissionsRequiredForLibrary(library: Library): Boolean
}
