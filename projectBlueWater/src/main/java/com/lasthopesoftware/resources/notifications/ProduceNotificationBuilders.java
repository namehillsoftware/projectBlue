package com.lasthopesoftware.resources.notifications;

import androidx.core.app.NotificationCompat;

public interface ProduceNotificationBuilders {
	NotificationCompat.Builder getNotificationBuilder(String notificationChannel);
}
