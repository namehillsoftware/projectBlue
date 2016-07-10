package com.lasthopesoftware.bluewater.client.library.permissions.storage.request;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsPendingIntentBuilder;
import com.lasthopesoftware.bluewater.client.settings.IEditClientSettingsPendingIntentBuilder;
import com.lasthopesoftware.resources.strings.IStringResourceProvider;
import com.lasthopesoftware.resources.strings.StringResourceProvider;

/**
 * Created by david on 7/10/16.
 */
public class StoragePermissionsRequestNotificationBuilder implements IStoragePermissionsRequestNotificationBuilder {

	private final NotificationCompat.Builder notificationBuilder;
	private final IStringResourceProvider stringResourceProvider;
	private final IEditClientSettingsPendingIntentBuilder editServerSettingsPendingIntentBuilder;

	public StoragePermissionsRequestNotificationBuilder(Context context) {
		this(new NotificationCompat.Builder(context), new StringResourceProvider(context), new EditClientSettingsPendingIntentBuilder(context));
	}

	public StoragePermissionsRequestNotificationBuilder(NotificationCompat.Builder notificationBuilder, IStringResourceProvider stringResourceProvider, IEditClientSettingsPendingIntentBuilder editServerSettingsPendingIntentBuilder) {
		this.notificationBuilder = notificationBuilder;
		this.stringResourceProvider = stringResourceProvider;
		this.editServerSettingsPendingIntentBuilder = editServerSettingsPendingIntentBuilder;
	}

	@Override
	public Notification buildStoragePermissionsRequestNotification(int libraryId) {
		notificationBuilder.setSmallIcon(R.drawable.clearstream_logo_dark);
		notificationBuilder.setContentTitle(stringResourceProvider.getString(R.string.permissions_needed));
		notificationBuilder.setContentText(stringResourceProvider.getString(R.string.permissions_needed_launch_settings));

		notificationBuilder.setContentIntent(editServerSettingsPendingIntentBuilder.buildEditServerSettingsPendingIntent(libraryId));

		return notificationBuilder.build();
	}
}
