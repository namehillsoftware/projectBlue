package com.lasthopesoftware.permissions.storage.write.request;

import android.app.Notification;

/**
 * Created by david on 7/3/16.
 */
public interface IStorageWritePermissionsRequestNotificationBuilder {
	Notification buildWritePermissionsRequestNotification(int libraryId);
}
