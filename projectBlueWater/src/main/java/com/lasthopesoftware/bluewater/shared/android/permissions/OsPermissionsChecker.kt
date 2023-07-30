package com.lasthopesoftware.bluewater.shared.android.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class OsPermissionsChecker(private val context: Context) : CheckOsPermissions {
	override val isReadMediaAudioPermissionGranted: Boolean
		get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
			|| isPermissionGranted(Manifest.permission.READ_MEDIA_AUDIO)

	override val isReadPermissionGranted: Boolean
		get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
			&& isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)

	override val isWritePermissionGranted: Boolean
		get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
			&& isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	override val isNotificationsPermissionGranted: Boolean
		get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
			|| isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)

	private fun isPermissionGranted(permission: String) =
		ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
