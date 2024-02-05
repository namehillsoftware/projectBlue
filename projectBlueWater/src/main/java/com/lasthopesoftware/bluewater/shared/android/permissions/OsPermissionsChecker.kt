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

	override val isNotificationsPermissionGranted: Boolean
		get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
			|| isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)

	override val isForegroundMediaServicePermissionNotGranted: Boolean
		get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
			&& !isPermissionGranted(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK)
	override val isForegroundDataServicePermissionNotGranted: Boolean
		get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
			&& !isPermissionGranted(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC)

	private fun isPermissionGranted(permission: String) =
		ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
