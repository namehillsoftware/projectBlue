package com.lasthopesoftware.bluewater.shared.android.notifications.control

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.util.SparseBooleanArray

class NotificationsController(
	private val service: Service,
	private val notificationManager: NotificationManager
) : ControlNotifications {

	private val syncObject = Any()
	private val notificationForegroundStatuses = SparseBooleanArray()

	override fun notifyEither(notification: Notification?, notificationId: Int) {
		synchronized(syncObject) {
			notificationForegroundStatuses.put(notificationId, notificationForegroundStatuses[notificationId, false])
			notificationManager.notify(notificationId, notification)
		}
	}

	override fun notifyBackground(notification: Notification?, notificationId: Int) {
		synchronized(syncObject) {
			if (isOnlyNotificationForeground(notificationId)) service.stopForeground(false)
			markNotificationBackground(notificationId)
			notificationManager.notify(notificationId, notification)
		}
	}

	override fun notifyForeground(notification: Notification?, notificationId: Int) {
		synchronized(syncObject) {
			service.startForeground(notificationId, notification)
			markNotificationForeground(notificationId)
		}
	}

	override fun removeAllNotifications() {
		synchronized(syncObject) {
			while (notificationForegroundStatuses.size() > 0)
				removeNotification(notificationForegroundStatuses.keyAt(0))
		}
	}

	override fun removeNotification(notificationId: Int) {
		synchronized(syncObject) {
			notificationForegroundStatuses.delete(notificationId)
			notificationManager.cancel(notificationId)
			if (isAllNotificationsBackground) service.stopForeground(true)
		}
	}

	override fun stopForegroundNotification(notificationId: Int) {
		synchronized(syncObject) {
			markNotificationBackground(notificationId)
			if (isAllNotificationsBackground) service.stopForeground(false)
		}
	}

	private fun isOnlyNotificationForeground(notificationId: Int): Boolean =
		synchronized(syncObject) {
			isNotificationForeground(notificationId) && isAllNotificationsBackgroundExcept(
				notificationId
			)
		}

	private fun isNotificationForeground(notificationId: Int): Boolean {
		synchronized(syncObject) { return notificationForegroundStatuses[notificationId, false] }
	}

	private val isAllNotificationsBackground: Boolean
		get() = isAllNotificationsBackgroundExcept(null)

	private fun isAllNotificationsBackgroundExcept(except: Int?): Boolean {
		synchronized(syncObject) {
			for (i in 0 until notificationForegroundStatuses.size()) {
				if (notificationForegroundStatuses.keyAt(i) == except) continue
				if (notificationForegroundStatuses.valueAt(i)) return false
			}
		}
		return true
	}

	private fun markNotificationBackground(notificationId: Int) {
		synchronized(syncObject) { notificationForegroundStatuses.put(notificationId, false) }
	}

	private fun markNotificationForeground(notificationId: Int) {
		synchronized(syncObject) { notificationForegroundStatuses.put(notificationId, true) }
	}
}
