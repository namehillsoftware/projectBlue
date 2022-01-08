package com.lasthopesoftware.bluewater.shared.android.notifications.control

import android.app.Notification

interface ControlNotifications {
    fun notifyEither(notification: Notification?, notificationId: Int)
    fun notifyBackground(notification: Notification?, notificationId: Int)
    fun notifyForeground(notification: Notification?, notificationId: Int)
    fun removeAllNotifications()
    fun removeNotification(notificationId: Int)
    fun stopForegroundNotification(notificationId: Int)
}
