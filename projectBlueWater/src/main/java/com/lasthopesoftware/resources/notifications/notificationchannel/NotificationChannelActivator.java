package com.lasthopesoftware.resources.notifications.notificationchannel;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class NotificationChannelActivator implements ActivateChannel {

	private final NotificationManager notificationManager;

	public NotificationChannelActivator(NotificationManager notificationManager) {
		this.notificationManager = notificationManager;
	}

	@Override
	public String activateChannel(ChannelConfiguration channelConfiguration) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelConfiguration.getChannelId();

		final NotificationChannel channel = new NotificationChannel(
			channelConfiguration.getChannelId(),
			channelConfiguration.getChannelName(),
			channelConfiguration.getChannelImportance());

		channel.setDescription(channelConfiguration.getChannelDescription());

		notificationManager.createNotificationChannel(channel);

		return channel.getId();
	}
}
