package com.lasthopesoftware.resources.notifications;

import android.content.Context;
import androidx.core.app.NotificationCompat;

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
