package com.lasthopesoftware.bluewater.client.library.repository.permissions.storage.request;

import android.app.Notification;

/**
 * Created by david on 7/10/16.
 */
public interface IStoragePermissionsRequestNotificationBuilder {
	Notification buildStoragePermissionsRequestNotification(int libraryId);
}
