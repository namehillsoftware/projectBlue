package com.lasthopesoftware.bluewater.shared.android.notifications.control;

import android.app.Notification;

public interface ControlNotifications {
	void notifyBackground(Notification notification, int notificationId);

	void notifyForeground(Notification notification, int notificationId);

	void removeAllNotifications();

	void removeNotification(int notificationId);

	void stopForegroundNotification(int notificationId);
}
