package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.pendingKillService
import com.lasthopesoftware.bluewater.shared.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.shared.android.notifications.ProduceNotificationBuilders
import com.namehillsoftware.handoff.promises.Promise

class PlaybackStartingNotificationBuilder(
	private val context: Context,
	private val produceNotificationBuilders: ProduceNotificationBuilders,
	private val configuration: NotificationsConfiguration,
	private val intentBuilder: BuildIntents,
) :
	BuildPlaybackStartingNotification {

	private val lazyPendingKillService = lazy { pendingKillService(context) }

	override fun promisePreparedPlaybackStartingNotification(): Promise<NotificationCompat.Builder> {
		return Promise(produceNotificationBuilders.getNotificationBuilder(configuration.notificationChannel)
			.setDeleteIntent(lazyPendingKillService.value)
			.addAction(0, context.getString(R.string.btn_cancel), lazyPendingKillService.value)
			.setOngoing(false)
			.setSound(null)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setColor(ContextCompat.getColor(context, R.color.project_blue_dark))
			.setContentIntent(buildNowPlayingActivityIntent())
			.setShowWhen(true)
			.setSmallIcon(R.drawable.now_playing_status_icon_white)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setContentTitle(context.getString(R.string.app_name))
			.setContentText(context.getString(R.string.lbl_starting_playback)))
	}

	private fun buildNowPlayingActivityIntent(): PendingIntent {
		// Set the notification area
		return intentBuilder.buildPendingNowPlayingIntent()
	}
}
