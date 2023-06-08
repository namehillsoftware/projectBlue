package com.lasthopesoftware.bluewater.client.browsing.library.request.read;

import android.app.Notification;
import android.content.Context;

import com.lasthopesoftware.bluewater.client.browsing.library.request.IStoragePermissionsRequestNotificationBuilder;
import com.lasthopesoftware.bluewater.client.browsing.library.request.StoragePermissionsRequestNotificationBuilder;

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
