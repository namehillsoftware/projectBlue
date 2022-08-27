package com.lasthopesoftware.resources.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat

class FakeNotificationCompatBuilder(context: Context, private val returnNotification: Notification) :
    NotificationCompat.Builder(context, returnNotification) {
    override fun build(): Notification {
        return returnNotification
    }

    companion object {
        @JvmStatic
		fun newFakeBuilder(context: Context, returnNotification: Notification): NotificationCompat.Builder {
            return FakeNotificationCompatBuilder(context, returnNotification)
        }
    }
}
