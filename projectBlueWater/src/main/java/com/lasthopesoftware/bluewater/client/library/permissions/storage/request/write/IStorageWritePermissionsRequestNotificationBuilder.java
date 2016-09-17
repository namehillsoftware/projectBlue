package com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write;

import android.app.Notification;

/**
 * Created by david on 7/3/16.
 */
public interface IStorageWritePermissionsRequestNotificationBuilder {
	Notification buildWritePermissionsRequestNotification(int libraryId);
}
