package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building

import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface BuildNowPlayingNotificationContent {
    fun promiseNowPlayingNotification(libraryId: LibraryId, serviceFile: ServiceFile, isPlaying: Boolean): Promise<NotificationCompat.Builder?>

    fun promiseLoadingNotification(libraryId: LibraryId, isPlaying: Boolean): Promise<NotificationCompat.Builder?>
}
