package com.lasthopesoftware.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

/**
 * Created by david on 6/13/16.
 */
public class ExternalStorageReadPermissionsArbitrator implements IExternalStorageReadPermissionsArbitrator {
	private final Context context;

	public ExternalStorageReadPermissionsArbitrator(Context context) {
		this.context = context;
	}

	@Override
	public boolean isExternalStorageReadPermissionsGranted() {
		return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}
}
