package com.lasthopesoftware.resources.notifications;

import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.resources.notifications.channel.ChannelConfiguration;

public class NotificationBuilderSupplier implements SupplyNotificationBuilders {

	private final Context context;
	private final ChannelConfiguration channelConfiguration;

	public NotificationBuilderSupplier(Context context, ChannelConfiguration channelConfiguration) {
		this.context = context;
		this.channelConfiguration = channelConfiguration;
	}

	@Override
	public NotificationCompat.Builder getNotificationBuilder() {
		return new NotificationCompat.Builder(
			context,
			channelConfiguration.getChannelId());
	}
}
