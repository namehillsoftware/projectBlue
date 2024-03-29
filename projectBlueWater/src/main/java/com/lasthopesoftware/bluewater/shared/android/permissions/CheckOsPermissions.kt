package com.lasthopesoftware.bluewater.shared.android.permissions

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

interface CheckOsPermissions {
	@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	val isReadMediaAudioPermissionGranted: Boolean

	val isReadPermissionGranted: Boolean

	@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	val isNotificationsPermissionNotGranted: Boolean
		get() = !isNotificationsPermissionGranted

	val isNotificationsPermissionGranted: Boolean

	@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	val isForegroundMediaServicePermissionNotGranted: Boolean

	@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	val isForegroundDataServicePermissionNotGranted: Boolean
}
