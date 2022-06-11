package com.lasthopesoftware.bluewater.client.browsing.library.request;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

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

	public StoragePermissionsRequestNotificationBuilder(@NonNull NotificationCompat.Builder notificationBuilder, @NonNull IStringResourceProvider stringResourceProvider, @NonNull IEditClientSettingsPendingIntentBuilder editServerSettingsPendingIntentBuilder) {
		this.notificationBuilder = notificationBuilder;
		this.stringResourceProvider = stringResourceProvider;
		this.editServerSettingsPendingIntentBuilder = editServerSettingsPendingIntentBuilder;
	}

	@NonNull
	@Override
	public Notification buildStoragePermissionsRequestNotification(int libraryId) {
		notificationBuilder.setSmallIcon(R.drawable.now_playing_status_icon_white);
		notificationBuilder.setContentTitle(stringResourceProvider.getString(R.string.permissions_needed));
		notificationBuilder.setContentText(stringResourceProvider.getString(R.string.permissions_needed_launch_settings));

		notificationBuilder.setContentIntent(editServerSettingsPendingIntentBuilder.buildEditServerSettingsPendingIntent(libraryId));

		notificationBuilder.setAutoCancel(true);

		notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		return notificationBuilder.build();
	}
}
