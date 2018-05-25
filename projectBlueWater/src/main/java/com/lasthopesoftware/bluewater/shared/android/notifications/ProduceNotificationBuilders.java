package com.lasthopesoftware.bluewater.shared.android.notifications;

import android.support.v4.app.NotificationCompat;

public interface ProduceNotificationBuilders {
	NotificationCompat.Builder getNotificationBuilder(String notificationChannel);
}
