package com.lasthopesoftware.permissions.storage.write.request;

import android.app.Notification;
import android.content.Context;

import com.lasthopesoftware.permissions.storage.request.IStoragePermissionsRequestNotificationBuilder;
import com.lasthopesoftware.permissions.storage.request.StoragePermissionsRequestNotificationBuilder;

/**
 * Created by david on 7/10/16.
 */
public class StorageWritePermissionsRequestNotificationBuilder implements IStorageWritePermissionsRequestNotificationBuilder {
	private final IStoragePermissionsRequestNotificationBuilder storagePermissionsRequestNotificationBuilder;

	public StorageWritePermissionsRequestNotificationBuilder(Context context) {
		this(new StoragePermissionsRequestNotificationBuilder(context));
	}

	public StorageWritePermissionsRequestNotificationBuilder(IStoragePermissionsRequestNotificationBuilder storagePermissionsRequestNotificationBuilder) {
		this.storagePermissionsRequestNotificationBuilder = storagePermissionsRequestNotificationBuilder;
	}

	@Override
	public Notification buildWritePermissionsRequestNotification(int libraryId) {
		return storagePermissionsRequestNotificationBuilder.buildStoragePermissionsRequestNotification(libraryId);
	}
}
