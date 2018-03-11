package com.lasthopesoftware.resources.notifications.channel;

import android.app.NotificationChannel;
import android.os.Build;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.O)
public class NotificationChannelBuilder implements BuildNotificationChannels {
	private final ChannelConfiguration channelConfiguration;

	public NotificationChannelBuilder(ChannelConfiguration channelConfiguration) {
		this.channelConfiguration = channelConfiguration;
	}

	@Override
	public NotificationChannel buildNotificationChannel() {
		NotificationChannel channel = new NotificationChannel(
			channelConfiguration.getChannelId(),
			channelConfiguration.getChannelName(),
			channelConfiguration.getChannelImportance());

		channel.setDescription(channelConfiguration.getChannelDescription());

		return channel;
	}
}
