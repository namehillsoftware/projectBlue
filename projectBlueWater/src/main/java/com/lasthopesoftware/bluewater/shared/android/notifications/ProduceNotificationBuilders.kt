package com.lasthopesoftware.bluewater.shared.android.notifications

import androidx.core.app.NotificationCompat

interface ProduceNotificationBuilders {
    fun getNotificationBuilder(notificationChannel: String): NotificationCompat.Builder
}
