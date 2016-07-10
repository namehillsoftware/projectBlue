package com.lasthopesoftware.permissions.storage.read.request;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.settings.EditServerSettingsPendingIntentBuilder;
import com.lasthopesoftware.bluewater.servers.settings.IEditServerSettingsPendingIntentBuilder;
import com.lasthopesoftware.permissions.storage.request.IStoragePermissionsRequestNotificationBuilder;
import com.lasthopesoftware.permissions.storage.request.StoragePermissionsRequestNotificationBuilder;
import com.lasthopesoftware.resources.strings.IStringResourceProvider;
import com.lasthopesoftware.resources.strings.StringResourceProvider;

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
