package com.lasthopesoftware.bluewater.client.playback.service.notification.building;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration;
import com.lasthopesoftware.resources.notifications.ProduceNotificationBuilders;
import com.namehillsoftware.handoff.promises.Promise;

public class PlaybackStartingNotificationBuilder implements BuildPlaybackStartingNotification {

	private final Context context;
	private final ProduceNotificationBuilders produceNotificationBuilders;
	private final NotificationsConfiguration configuration;
	private final MediaSessionCompat mediaSessionCompat;

	public PlaybackStartingNotificationBuilder(Context context, ProduceNotificationBuilders produceNotificationBuilders, NotificationsConfiguration configuration, MediaSessionCompat mediaSessionCompat) {
		this.context = context;
		this.produceNotificationBuilders = produceNotificationBuilders;
		this.configuration = configuration;
		this.mediaSessionCompat = mediaSessionCompat;
	}

	@Override
	public Promise<NotificationCompat.Builder> promisePreparedPlaybackStartingNotification() {
		return new Promise<>(produceNotificationBuilders.getNotificationBuilder(configuration.getNotificationChannel())
			.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
				.setCancelButtonIntent(PlaybackService.pendingKillService(context))
				.setMediaSession(mediaSessionCompat.getSessionToken())
				.setShowCancelButton(true))
			.setOngoing(false)
			.setSound(null)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setColor(ContextCompat.getColor(context, R.color.clearstream_dark))
			.setContentIntent(buildNowPlayingActivityIntent())
			.setShowWhen(true)
			.setSmallIcon(R.drawable.clearstream_logo_dark)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setContentTitle(context.getString(R.string.app_name))
			.setContentText(context.getString(R.string.lbl_starting_playback)));
	}

	private PendingIntent buildNowPlayingActivityIntent() {
		// Set the notification area
		final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(context, 0, viewIntent, 0);
	}
}
