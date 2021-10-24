package com.lasthopesoftware.bluewater.client.playback.service.notification.building

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingKillService
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.NowPlayingActivity
import com.lasthopesoftware.bluewater.shared.android.notifications.ProduceNotificationBuilders
import com.lasthopesoftware.bluewater.shared.makePendingIntentImmutable

class MediaStyleNotificationSetup(
	private val context: Context,
	private val produceNotificationBuilders: ProduceNotificationBuilders,
	private val configuration: NotificationsConfiguration,
	private val mediaSessionCompat: MediaSessionCompat
) : SetupMediaStyleNotifications {
	private val pendingNowPlayingIntent by lazy {
		// Set the notification area
		val viewIntent = Intent(context, NowPlayingActivity::class.java)
		viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
		PendingIntent.getActivity(context, 0, viewIntent, 0.makePendingIntentImmutable())
	}

	override fun getMediaStyleNotification(): NotificationCompat.Builder {
		val builder = produceNotificationBuilders.getNotificationBuilder(configuration.notificationChannel)
		return builder
			.setStyle(
				androidx.media.app.NotificationCompat.MediaStyle()
					.setCancelButtonIntent(pendingKillService(context))
					.setMediaSession(mediaSessionCompat.sessionToken)
					.setShowActionsInCompactView(1)
					.setShowCancelButton(true)
			)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setColor(ContextCompat.getColor(context, R.color.clearstream_dark))
			.setContentIntent(pendingNowPlayingIntent)
			.setDeleteIntent(pendingKillService(context))
			.setShowWhen(false)
			.setSmallIcon(R.drawable.clearstream_logo_dark)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
	}
}
