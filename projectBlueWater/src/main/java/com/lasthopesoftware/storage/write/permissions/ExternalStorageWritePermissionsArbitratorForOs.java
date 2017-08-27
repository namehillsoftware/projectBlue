package com.lasthopesoftware.storage.write.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

public class ExternalStorageWritePermissionsArbitratorForOs implements IStorageWritePermissionArbitratorForOs {
	private final Context context;

	public ExternalStorageWritePermissionsArbitratorForOs(Context context) {
		this.context = context;
	}

	@Override
	public boolean isWritePermissionGranted() {
		return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}
}
