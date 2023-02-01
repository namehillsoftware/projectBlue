package com.lasthopesoftware.bluewater.shared.android.notifications.control

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.os.Build
import android.util.SparseBooleanArray

class NotificationsController(
	private val service: Service,
	private val notificationManager: NotificationManager
) : ControlNotifications {

	private val syncObject = Any()
	private val notificationForegroundStatuses = SparseBooleanArray()

	override fun notifyEither(notification: Notification?, notificationId: Int) {
		synchronized(syncObject) {
			if (notificationForegroundStatuses.indexOfKey(notificationId) < 0)
				notificationForegroundStatuses.put(notificationId, false)

			notificationManager.notify(notificationId, notification)
		}
	}

	override fun notifyBackground(notification: Notification?, notificationId: Int) {
		synchronized(syncObject) {
			if (isOnlyNotificationForeground(notificationId)) stopForeground(false)
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
			if (isAllNotificationsBackground) stopForeground(true)
		}
	}

	override fun stopForegroundNotification(notificationId: Int) {
		synchronized(syncObject) {
			markNotificationBackground(notificationId)
			if (isAllNotificationsBackground) stopForeground(false)
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

	private fun stopForeground(removeNotification: Boolean) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			service.stopForeground(
				if (removeNotification) Service.STOP_FOREGROUND_REMOVE
				else Service.STOP_FOREGROUND_DETACH
			)
		} else {
			service.stopForeground(removeNotification)
		}
	}
}
