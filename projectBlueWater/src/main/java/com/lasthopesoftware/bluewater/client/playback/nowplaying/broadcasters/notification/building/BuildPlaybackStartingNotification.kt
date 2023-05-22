package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building

import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface BuildPlaybackStartingNotification {
    fun promisePreparedPlaybackStartingNotification(libraryId: LibraryId): Promise<NotificationCompat.Builder>
}
