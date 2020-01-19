package com.lasthopesoftware.resources.notifications.notificationchannel;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.O)
public class NotificationChannelActivator implements ActivateChannel {

	private final NotificationManager notificationManager;

	public NotificationChannelActivator(NotificationManager notificationManager) {
		this.notificationManager = notificationManager;
	}

	@Override
	public String activateChannel(ChannelConfiguration channelConfiguration) {
		final NotificationChannel channel = new NotificationChannel(
			channelConfiguration.getChannelId(),
			channelConfiguration.getChannelName(),
			channelConfiguration.getChannelImportance());

		channel.setSound(null, null);

		channel.setDescription(channelConfiguration.getChannelDescription());

		notificationManager.createNotificationChannel(channel);

		return channel.getId();
	}
}
