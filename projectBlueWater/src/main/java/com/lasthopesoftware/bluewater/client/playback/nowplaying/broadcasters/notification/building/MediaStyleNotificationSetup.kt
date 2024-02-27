package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.shared.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.shared.android.notifications.ProduceNotificationBuilders

@UnstableApi class MediaStyleNotificationSetup(
	private val context: Context,
	private val produceNotificationBuilders: ProduceNotificationBuilders,
	private val configuration: NotificationsConfiguration,
	private val mediaSessionCompat: MediaSessionCompat,
	private val intentBuilder: BuildIntents,
) : SetupMediaStyleNotifications {
	override fun getMediaStyleNotification(libraryId: LibraryId): NotificationCompat.Builder {
		val builder = produceNotificationBuilders.getNotificationBuilder(configuration.notificationChannel)
		val intent = intentBuilder.buildPendingNowPlayingIntent(libraryId)
		val pauseIntent = intentBuilder.buildPendingPausePlaybackIntent()
		return builder
			.setStyle(
				androidx.media.app.NotificationCompat.MediaStyle()
					.setCancelButtonIntent(pauseIntent)
					.setMediaSession(mediaSessionCompat.sessionToken)
					.setShowActionsInCompactView(1)
					.setShowCancelButton(true)
			)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setColor(ContextCompat.getColor(context, R.color.project_blue_dark))
			.setContentIntent(intent)
			.setDeleteIntent(pauseIntent)
			.setShowWhen(false)
			.setSmallIcon(R.drawable.now_playing_status_icon_white)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
	}
}
