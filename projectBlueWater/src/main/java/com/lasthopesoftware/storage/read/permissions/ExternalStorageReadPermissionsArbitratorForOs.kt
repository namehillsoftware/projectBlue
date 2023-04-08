package com.lasthopesoftware.storage.read.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class ExternalStorageReadPermissionsArbitratorForOs(private val context: Context) :
    IStorageReadPermissionArbitratorForOs {
    override fun isReadPermissionGranted(): Boolean {
        return ContextCompat
			.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}
