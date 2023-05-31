package com.lasthopesoftware.bluewater.shared.android.notifications

import android.content.Context
import androidx.core.app.NotificationCompat

class NotificationBuilderProducer(private val context: Context) : ProduceNotificationBuilders {
    override fun getNotificationBuilder(notificationChannel: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, notificationChannel)
    }
}
