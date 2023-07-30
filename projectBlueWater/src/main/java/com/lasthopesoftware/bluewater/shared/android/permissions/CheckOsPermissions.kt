package com.lasthopesoftware.bluewater.shared.android.permissions

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

interface CheckOsPermissions {
	@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	val isReadMediaAudioPermissionGranted: Boolean
    val isReadPermissionGranted: Boolean
	val isWritePermissionGranted: Boolean
	val isNotificationsPermissionGranted: Boolean
}
