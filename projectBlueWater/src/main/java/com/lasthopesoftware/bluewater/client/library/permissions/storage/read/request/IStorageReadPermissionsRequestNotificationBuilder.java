package com.lasthopesoftware.bluewater.client.library.permissions.storage.read.request;

import android.app.Notification;

/**
 * Created by david on 7/3/16.
 */
public interface IStorageReadPermissionsRequestNotificationBuilder {
	Notification buildReadPermissionsRequestNotification(int libraryId);
}
