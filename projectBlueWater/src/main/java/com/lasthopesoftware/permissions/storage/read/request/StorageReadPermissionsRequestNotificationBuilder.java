package com.lasthopesoftware.permissions.storage.read.request;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.settings.EditServerSettingsPendingIntentBuilder;
import com.lasthopesoftware.bluewater.servers.settings.IEditServerSettingsPendingIntentBuilder;
import com.lasthopesoftware.resources.strings.IStringResourceProvider;
import com.lasthopesoftware.resources.strings.StringResourceProvider;

/**
 * Created by david on 7/3/16.
 */
public class StorageReadPermissionsRequestNotificationBuilder implements IStorageReadPermissionsRequestNotificationBuilder {
	private final NotificationCompat.Builder notificationBuilder;
	private final IStringResourceProvider stringResourceProvider;
	private final IEditServerSettingsPendingIntentBuilder editServerSettingsPendingIntentBuilder;

	public StorageReadPermissionsRequestNotificationBuilder(Context context) {
		this(new NotificationCompat.Builder(context), new StringResourceProvider(context), new EditServerSettingsPendingIntentBuilder(context));
	}

	public StorageReadPermissionsRequestNotificationBuilder(NotificationCompat.Builder notificationBuilder, IStringResourceProvider stringResourceProvider, IEditServerSettingsPendingIntentBuilder editServerSettingsPendingIntentBuilder) {
		this.notificationBuilder = notificationBuilder;
		this.stringResourceProvider = stringResourceProvider;
		this.editServerSettingsPendingIntentBuilder = editServerSettingsPendingIntentBuilder;
	}

	@Override
	public Notification buildReadPermissionsRequestNotification(int libraryId) {
		notificationBuilder.setSmallIcon(R.drawable.clearstream_logo_dark);
		notificationBuilder.setContentTitle(stringResourceProvider.getString(R.string.permissions_needed));
		notificationBuilder.setContentText(stringResourceProvider.getString(R.string.permissions_needed_launch_settings));

		notificationBuilder.setContentIntent(editServerSettingsPendingIntentBuilder.buildEditServerSettingsPendingIntent(libraryId));

		return notificationBuilder.build();
	}
}
