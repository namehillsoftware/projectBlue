package com.lasthopesoftware.storage.read.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

public class ExternalStorageReadPermissionsArbitratorForOs implements IStorageReadPermissionArbitratorForOs {
	private final Context context;

	public ExternalStorageReadPermissionsArbitratorForOs(Context context) {
		this.context = context;
	}

	@Override
	public boolean isReadPermissionGranted() {
		return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}
}
