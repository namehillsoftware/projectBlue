package com.lasthopesoftware.bluewater.client.playback.service.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.namehillsoftware.handoff.promises.Promise;

public class NowPlayingNotificationBuilder implements BuildNowPlayingNotificationContent {

	private final Context context;
	private final MediaSessionCompat mediaSessionCompat;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private final PlaybackNotificationsConfiguration configuration;

	public NowPlayingNotificationBuilder(Context context, MediaSessionCompat mediaSessionCompat, CachedFilePropertiesProvider cachedFilePropertiesProvider, PlaybackNotificationsConfiguration configuration) {
		this.context = context;
		this.mediaSessionCompat = mediaSessionCompat;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		this.configuration = configuration;
	}

	@Override
	public Promise<Notification> promiseNowPlayingNotification(ServiceFile serviceFile, boolean isPlaying) {
		return cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey())
			.then(fileProperties -> {
				final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
				final String name = fileProperties.get(FilePropertiesProvider.NAME);

				final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, configuration.getNotificationChannel());
				builder
					.setStyle(new android.support.v4.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
						.setCancelButtonIntent(PlaybackService.pendingKillService(context))
						.setMediaSession(mediaSessionCompat.getSessionToken())
						.setShowActionsInCompactView(1))
					.setOngoing(isPlaying)
					.setContentTitle(artist + " - " + name)
					.setContentText(String.format(context.getString(R.string.title_svc_now_playing), context.getText(R.string.app_name)))
					.setColor(NotificationCompat.COLOR_DEFAULT)
					.setContentIntent(buildNowPlayingActivityIntent())
					.setShowWhen(false)
					.setDeleteIntent(PlaybackService.pendingKillService(context))
					.addAction(new NotificationCompat.Action(
						R.drawable.av_rewind,
						context.getString(R.string.btn_previous),
						PlaybackService.pendingPreviousIntent(context)))
					.addAction(isPlaying
						? new NotificationCompat.Action(
						R.drawable.av_pause,
						context.getString(R.string.btn_pause),
						PlaybackService.pendingPauseIntent(context))
						: new NotificationCompat.Action(
						R.drawable.av_play,
						context.getString(R.string.btn_play),
						PlaybackService.pendingPlayingIntent(context)))
					.addAction(new NotificationCompat.Action(
						R.drawable.av_fast_forward,
						context.getString(R.string.btn_next),
						PlaybackService.pendingNextIntent(context)))
					.setSmallIcon(R.drawable.clearstream_logo_dark)
					.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

				return builder.build();
			});
	}

	private PendingIntent buildNowPlayingActivityIntent() {
		// Set the notification area
		final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(context, 0, viewIntent, 0);
	}
}
