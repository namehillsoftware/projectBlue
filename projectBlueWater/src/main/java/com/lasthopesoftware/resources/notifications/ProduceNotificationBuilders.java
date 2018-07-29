package com.lasthopesoftware.resources.notifications;

import android.support.v4.app.NotificationCompat;

public interface ProduceNotificationBuilders {
	NotificationCompat.Builder getNotificationBuilder(String notificationChannel);
}
