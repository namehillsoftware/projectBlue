package com.lasthopesoftware.bluewater.client.browsing.library.request.write;

import android.app.Notification;
import android.content.Context;

import com.lasthopesoftware.bluewater.client.browsing.library.request.IStoragePermissionsRequestNotificationBuilder;
import com.lasthopesoftware.bluewater.client.browsing.library.request.StoragePermissionsRequestNotificationBuilder;

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
