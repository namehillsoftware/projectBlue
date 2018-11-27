package com.lasthopesoftware.bluewater.client.playback.service.notification.building;

import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaSessionCompat;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.resources.notifications.ProduceNotificationBuilders;

public class MediaStyleNotificationSetup implements SetupMediaStyleNotifications {

	private final Context context;
	private final ProduceNotificationBuilders produceNotificationBuilders;
	private final PlaybackNotificationsConfiguration configuration;
	private final MediaSessionCompat mediaSessionCompat;

	public MediaStyleNotificationSetup(Context context, ProduceNotificationBuilders produceNotificationBuilders, PlaybackNotificationsConfiguration configuration, MediaSessionCompat mediaSessionCompat) {
		this.context = context;
		this.produceNotificationBuilders = produceNotificationBuilders;
		this.configuration = configuration;
		this.mediaSessionCompat = mediaSessionCompat;
	}

	@Override
	public NotificationCompat.Builder getMediaStyleNotification() {
		final NotificationCompat.Builder builder = produceNotificationBuilders.getNotificationBuilder(configuration.getNotificationChannel());

		builder
			.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
				.setCancelButtonIntent(PlaybackService.pendingKillService(context))
				.setMediaSession(mediaSessionCompat.getSessionToken())
				.setShowActionsInCompactView(1)
				.setShowCancelButton(true));
		return builder;
	}
}
