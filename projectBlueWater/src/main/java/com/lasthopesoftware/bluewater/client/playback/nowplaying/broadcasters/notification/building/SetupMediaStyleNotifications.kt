package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building

import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

interface SetupMediaStyleNotifications {
	fun getMediaStyleNotification(libraryId: LibraryId): NotificationCompat.Builder
}
