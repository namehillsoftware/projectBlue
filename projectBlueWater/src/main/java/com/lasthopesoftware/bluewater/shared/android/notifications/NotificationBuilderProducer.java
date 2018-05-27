package com.lasthopesoftware.bluewater.shared.android.notifications;

import android.content.Context;
import android.support.v4.app.NotificationCompat;

public class NotificationBuilderProducer implements ProduceNotificationBuilders {

	private final Context context;

	public NotificationBuilderProducer(Context context) {
		this.context = context;
	}

	@Override
	public NotificationCompat.Builder getNotificationBuilder(String notificationChannel) {
		return new NotificationCompat.Builder(context, notificationChannel);
	}
}
