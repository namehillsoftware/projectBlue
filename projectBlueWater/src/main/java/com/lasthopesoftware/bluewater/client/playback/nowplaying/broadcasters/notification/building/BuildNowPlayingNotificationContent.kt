package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building

import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface BuildNowPlayingNotificationContent {
    fun promiseNowPlayingNotification(serviceFile: ServiceFile, isPlaying: Boolean): Promise<NotificationCompat.Builder>

    fun getLoadingNotification(isPlaying: Boolean): NotificationCompat.Builder
}
