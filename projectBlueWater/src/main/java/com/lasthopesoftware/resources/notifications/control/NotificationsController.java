package com.lasthopesoftware.resources.notifications.control;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.util.SparseBooleanArray;

public class NotificationsController implements ControlNotifications {

	private final SparseBooleanArray notificationForegroundStatuses = new SparseBooleanArray();
	private final Service service;
	private final NotificationManager notificationManager;

	public NotificationsController(Service service, NotificationManager notificationManager) {
		this.service = service;
		this.notificationManager = notificationManager;
	}

	private boolean isOnlyNotificationForeground(int notificationId) {
		return isNotificationForeground(notificationId)	&& !isAnyNotificationForeground();
	}

	private boolean isNotificationForeground(int notificationId) {
		return notificationForegroundStatuses.get(notificationId, false);
	}

	@Override
	public void notifyBackground(Notification notification, int notificationId) {
		if (isOnlyNotificationForeground(notificationId))
			service.stopForeground(false);

		markNotificationBackground(notificationId);

		notificationManager.notify(notificationId, notification);
	}

	@Override
	public void notifyForeground(Notification notification, int notificationId) {
		if (isNotificationForeground(notificationId)) {
			notificationManager.notify(notificationId, notification);
			return;
		}

		service.startForeground(notificationId, notification);
		markNotificationForeground(notificationId);
	}

	@Override
	public void stopAllForegroundNotifications() {
		for (int i = 0; i < notificationForegroundStatuses.size(); i++)
			stopForegroundNotification(notificationForegroundStatuses.keyAt(i));
	}

	@Override
	public void stopForegroundNotification(int notificationId) {
		markNotificationBackground(notificationId);
		if (!isAnyNotificationForeground()) service.stopForeground(true);
		notificationManager.cancel(notificationId);
	}

	private boolean isAnyNotificationForeground() {
		for (int i = 0; i < notificationForegroundStatuses.size(); i++) {
			if (notificationForegroundStatuses.valueAt(i)) return true;
		}

		return false;
	}

	private void markNotificationBackground(int notificationId) {
		notificationForegroundStatuses.put(notificationId, false);
	}

	private void markNotificationForeground(int notificationId) {
		notificationForegroundStatuses.put(notificationId, false);
	}
}
