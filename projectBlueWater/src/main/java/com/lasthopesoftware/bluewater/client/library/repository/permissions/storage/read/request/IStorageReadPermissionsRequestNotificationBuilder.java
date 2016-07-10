package com.lasthopesoftware.bluewater.client.library.repository.permissions.storage.read.request;

import android.app.Notification;

/**
 * Created by david on 7/3/16.
 */
public interface IStorageReadPermissionsRequestNotificationBuilder {
	Notification buildReadPermissionsRequestNotification(int libraryId);
}
