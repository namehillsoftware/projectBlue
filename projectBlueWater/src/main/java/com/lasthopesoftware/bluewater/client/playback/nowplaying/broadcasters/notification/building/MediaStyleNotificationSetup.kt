package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building

import android.app.PendingIntent
import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingKillService
import com.lasthopesoftware.bluewater.shared.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.shared.android.makePendingIntentImmutable
import com.lasthopesoftware.bluewater.shared.android.notifications.ProduceNotificationBuilders

class MediaStyleNotificationSetup(
	private val context: Context,
	private val produceNotificationBuilders: ProduceNotificationBuilders,
	private val configuration: NotificationsConfiguration,
	private val mediaSessionCompat: MediaSessionCompat,
	private val intentBuilder: BuildIntents,
) : SetupMediaStyleNotifications {
	override fun getMediaStyleNotification(): NotificationCompat.Builder {
		val builder = produceNotificationBuilders.getNotificationBuilder(configuration.notificationChannel)
		val intent = intentBuilder.buildNowPlayingIntent()
		val taskStackBuilder =
		return builder
			.setStyle(
				androidx.media.app.NotificationCompat.MediaStyle()
					.setCancelButtonIntent(pendingKillService(context))
					.setMediaSession(mediaSessionCompat.sessionToken)
					.setShowActionsInCompactView(1)
					.setShowCancelButton(true)
			)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setColor(ContextCompat.getColor(context, R.color.project_blue_dark))
			.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0.makePendingIntentImmutable()))
			.setDeleteIntent(pendingKillService(context))
			.setShowWhen(false)
			.setSmallIcon(R.drawable.now_playing_status_icon_white)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
	}
}
