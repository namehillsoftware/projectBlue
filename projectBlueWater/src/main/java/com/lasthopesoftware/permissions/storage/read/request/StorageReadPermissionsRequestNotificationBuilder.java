package com.lasthopesoftware.permissions.storage.read.request;

import android.app.Notification;
import android.content.Context;

import com.lasthopesoftware.permissions.storage.request.IStoragePermissionsRequestNotificationBuilder;
import com.lasthopesoftware.permissions.storage.request.StoragePermissionsRequestNotificationBuilder;

/**
 * Created by david on 7/3/16.
 */
public class StorageReadPermissionsRequestNotificationBuilder implements IStorageReadPermissionsRequestNotificationBuilder {
	private final IStoragePermissionsRequestNotificationBuilder storagePermissionsRequestNotificationBuilder;

	public StorageReadPermissionsRequestNotificationBuilder(Context context) {
		this(new StoragePermissionsRequestNotificationBuilder(context));
	}

	public StorageReadPermissionsRequestNotificationBuilder(IStoragePermissionsRequestNotificationBuilder storagePermissionsRequestNotificationBuilder) {
		this.storagePermissionsRequestNotificationBuilder = storagePermissionsRequestNotificationBuilder;
	}

	@Override
	public Notification buildReadPermissionsRequestNotification(int libraryId) {
		return storagePermissionsRequestNotificationBuilder.buildStoragePermissionsRequestNotification(libraryId);
	}
}
