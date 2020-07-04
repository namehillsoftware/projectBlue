package com.lasthopesoftware.bluewater.client.playback.service.notification.building;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.resources.notifications.ProduceNotificationBuilders;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class MediaStyleNotificationSetup implements SetupMediaStyleNotifications {

	private final Context context;
	private final ProduceNotificationBuilders produceNotificationBuilders;
	private final NotificationsConfiguration configuration;
	private final MediaSessionCompat mediaSessionCompat;
	private final CreateAndHold<PendingIntent> pendingNowPlayingIntent = new AbstractSynchronousLazy<PendingIntent>() {
		@Override
		protected PendingIntent create() throws Throwable {
			// Set the notification area
			final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
			viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			return PendingIntent.getActivity(context, 0, viewIntent, 0);
		}
	};

	public MediaStyleNotificationSetup(Context context, ProduceNotificationBuilders produceNotificationBuilders, NotificationsConfiguration configuration, MediaSessionCompat mediaSessionCompat) {
		this.context = context;
		this.produceNotificationBuilders = produceNotificationBuilders;
		this.configuration = configuration;
		this.mediaSessionCompat = mediaSessionCompat;
	}

	@Override
	public NotificationCompat.Builder getMediaStyleNotification() {
		final NotificationCompat.Builder builder = produceNotificationBuilders.getNotificationBuilder(configuration.getNotificationChannel());

		return builder
			.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
			.setCancelButtonIntent(PlaybackService.pendingKillService(context))
			.setMediaSession(mediaSessionCompat.getSessionToken())
			.setShowActionsInCompactView(1)
			.setShowCancelButton(true))
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setColor(ContextCompat.getColor(context, R.color.clearstream_dark))
			.setContentIntent(pendingNowPlayingIntent.getObject())
			.setDeleteIntent(PlaybackService.pendingKillService(context))
			.setShowWhen(false)
			.setSmallIcon(R.drawable.clearstream_logo_dark)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
	}
}
