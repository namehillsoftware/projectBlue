package com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
class NotificationChannelActivator(private val notificationManager: NotificationManager) :
    ActivateChannel {
    override fun activateChannel(channelConfiguration: ChannelConfiguration): String {
        val channel = NotificationChannel(
            channelConfiguration.channelId,
            channelConfiguration.channelName,
            channelConfiguration.channelImportance
        )
        channel.setSound(null, null)
        channel.description = channelConfiguration.channelDescription
        notificationManager.createNotificationChannel(channel)
        return channel.id
    }
}
