package com.lasthopesoftware.bluewater.shared.android.permissions

interface CheckOsPermissions {
    val isReadPermissionGranted: Boolean
	val isWritePermissionGranted: Boolean
	val isNotificationsPermissionGranted: Boolean
}
