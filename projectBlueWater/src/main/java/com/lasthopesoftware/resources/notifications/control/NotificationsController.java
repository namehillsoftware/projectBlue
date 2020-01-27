package com.lasthopesoftware.resources.notifications.control;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.util.SparseBooleanArray;

public class NotificationsController implements ControlNotifications {

	private final Object syncObject = new Object();
	private final SparseBooleanArray notificationForegroundStatuses = new SparseBooleanArray();
	private final Service service;
	private final NotificationManager notificationManager;

	public NotificationsController(Service service, NotificationManager notificationManager) {
		this.service = service;
		this.notificationManager = notificationManager;
	}

	@Override
	public void notifyBackground(Notification notification, int notificationId) {
		synchronized (syncObject) {
			if (isOnlyNotificationForeground(notificationId))
				service.stopForeground(false);

			markNotificationBackground(notificationId);

			notificationManager.notify(notificationId, notification);
		}
	}

	@Override
	public void notifyForeground(Notification notification, int notificationId) {
		synchronized (syncObject) {
			if (notificationForegroundStatuses.get(notificationId, false))
				notificationManager.notify(notificationId, notification);
			else
				service.startForeground(notificationId, notification);

			markNotificationForeground(notificationId);
		}
	}

	@Override
	public void removeAllNotifications() {
		synchronized (syncObject) {
			while (notificationForegroundStatuses.size() > 0)
				removeNotification(notificationForegroundStatuses.keyAt(0));
		}
	}

	@Override
	public void removeNotification(int notificationId) {
		synchronized (syncObject) {
			notificationForegroundStatuses.delete(notificationId);
			notificationManager.cancel(notificationId);
			if (isAllNotificationsBackground()) service.stopForeground(true);
		}
	}

	@Override
	public void stopForegroundNotification(int notificationId) {
		synchronized (syncObject) {
			markNotificationBackground(notificationId);
			if (isAllNotificationsBackground()) service.stopForeground(false);
		}
	}

	private boolean isOnlyNotificationForeground(int notificationId) {
		synchronized (syncObject) {
			return isNotificationForeground(notificationId) && isAllNotificationsBackgroundExcept(notificationId);
		}
	}

	private boolean isNotificationForeground(int notificationId) {
		synchronized (syncObject) {
			return notificationForegroundStatuses.get(notificationId, false);
		}
	}

	private boolean isAllNotificationsBackground() {
		return isAllNotificationsBackgroundExcept(null);
	}

	private boolean isAllNotificationsBackgroundExcept(Integer except) {
		synchronized (syncObject) {
			for (int i = 0; i < notificationForegroundStatuses.size(); i++) {
				if (except != null && notificationForegroundStatuses.keyAt(i) == except) continue;
				if (notificationForegroundStatuses.valueAt(i)) return false;
			}
		}

		return true;
	}

	private void markNotificationBackground(int notificationId) {
		synchronized (syncObject) {
			notificationForegroundStatuses.put(notificationId, false);
		}
	}

	private void markNotificationForeground(int notificationId) {
		synchronized (syncObject) {
			notificationForegroundStatuses.put(notificationId, true);
		}
	}
}
