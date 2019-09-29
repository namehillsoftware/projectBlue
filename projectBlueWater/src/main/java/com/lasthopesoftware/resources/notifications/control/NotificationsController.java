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

	private boolean isOnlyNotificationForeground(int notificationId) {
		synchronized (syncObject) {
			return isNotificationForeground(notificationId) && !isAnyNotificationForegroundExcept(notificationId);
		}
	}

	private boolean isNotificationForeground(int notificationId) {
		synchronized (syncObject) {
			return notificationForegroundStatuses.get(notificationId, false);
		}
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
			if (isNotificationForeground(notificationId)) {
				notificationManager.notify(notificationId, notification);
				return;
			}

			service.startForeground(notificationId, notification);
			markNotificationForeground(notificationId);
		}
	}

	@Override
	public void removeAllForegroundNotifications() {
		synchronized (syncObject) {
			for (int i = 0; i < notificationForegroundStatuses.size(); i++)
				removeForegroundNotification(notificationForegroundStatuses.keyAt(i));
		}
	}

	@Override
	public void removeForegroundNotification(int notificationId) {
		synchronized (syncObject) {
			markNotificationBackground(notificationId);
			if (!isAnyNotificationForeground()) service.stopForeground(true);
			notificationManager.cancel(notificationId);
		}
	}

	@Override
	public void stopForegroundNotification(int notificationId) {
		synchronized (syncObject) {
			markNotificationBackground(notificationId);
			if (!isAnyNotificationForeground()) service.stopForeground(false);
		}
	}

	private boolean isAnyNotificationForeground() {
		return isAnyNotificationForegroundExcept(null);
	}

	private boolean isAnyNotificationForegroundExcept(Integer except) {
		synchronized (syncObject) {
			for (int i = 0; i < notificationForegroundStatuses.size(); i++) {
				if (except != null && notificationForegroundStatuses.keyAt(i) == except) continue;
				if (notificationForegroundStatuses.valueAt(i)) return true;
			}
		}

		return false;
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
