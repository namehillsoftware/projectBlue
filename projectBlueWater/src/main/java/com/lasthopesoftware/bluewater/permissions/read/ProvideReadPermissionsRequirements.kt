package com.lasthopesoftware.bluewater.permissions.read

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings

interface ProvideReadPermissionsRequirements {
    fun isReadPermissionsRequiredForLibrary(librarySettings: LibrarySettings): Boolean

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	fun isReadMediaPermissionsRequiredForLibrary(library: LibrarySettings): Boolean
}
