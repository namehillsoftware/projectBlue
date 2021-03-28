package com.lasthopesoftware.bluewater.shared.android.notifications;

import androidx.core.app.NotificationCompat;

public interface ProduceNotificationBuilders {
	NotificationCompat.Builder getNotificationBuilder(String notificationChannel);
}
