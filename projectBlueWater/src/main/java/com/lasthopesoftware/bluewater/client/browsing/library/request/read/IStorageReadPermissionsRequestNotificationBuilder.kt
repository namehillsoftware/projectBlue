package com.lasthopesoftware.bluewater.client.browsing.library.request.read;

import android.app.Notification;

/**
 * Created by david on 7/3/16.
 */
public interface IStorageReadPermissionsRequestNotificationBuilder {
	Notification buildReadPermissionsRequestNotification(int libraryId);
}
