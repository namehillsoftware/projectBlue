package com.lasthopesoftware.resources.notifications.control;

import android.app.Notification;

public interface ControlNotifications {
	void notifyBackground(Notification notification, int notificationId);

	void notifyForeground(Notification notification, int notificationId);

	void removeAllForegroundNotifications();

	void removeForegroundNotification(int notificationId);

	void stopForegroundNotification(int notificationId);
}
