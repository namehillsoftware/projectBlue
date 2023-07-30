package com.lasthopesoftware.bluewater.permissions.read

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library

interface ProvideReadPermissionsRequirements {
    fun isReadPermissionsRequiredForLibrary(library: Library): Boolean

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	fun isReadMediaPermissionsRequiredForLibrary(library: Library): Boolean
}
