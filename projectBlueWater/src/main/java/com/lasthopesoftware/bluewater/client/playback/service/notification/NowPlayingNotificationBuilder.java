package com.lasthopesoftware.bluewater.client.playback.service.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.util.Map;

public class NowPlayingNotificationBuilder
implements
	BuildNowPlayingNotificationContent {

	private final Context context;
	private final MediaSessionCompat mediaSessionCompat;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private final ImageProvider imageProvider;
	private final PlaybackNotificationsConfiguration configuration;

	private Bitmap fileImage;

	public NowPlayingNotificationBuilder(Context context, MediaSessionCompat mediaSessionCompat, CachedFilePropertiesProvider cachedFilePropertiesProvider, ImageProvider imageProvider, PlaybackNotificationsConfiguration configuration) {
		this.context = context;
		this.mediaSessionCompat = mediaSessionCompat;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		this.imageProvider = imageProvider;
		this.configuration = configuration;
	}

	@Override
	public Promise<Notification> promiseNowPlayingNotification(ServiceFile serviceFile, boolean isPlaying) {
		return imageProvider.promiseFileBitmap(serviceFile)
			.eventually(
				bitmap -> cachedFilePropertiesProvider
					.promiseFileProperties(serviceFile.getKey())
					.then(new FilePropertiesReceivedResponse(isPlaying, updateBitmap(bitmap), context, mediaSessionCompat, configuration)),
				e -> cachedFilePropertiesProvider
					.promiseFileProperties(serviceFile.getKey())
					.then(new FilePropertiesReceivedResponse(isPlaying, updateBitmap(null), context, mediaSessionCompat, configuration)));
	}

	private synchronized Bitmap updateBitmap(Bitmap bitmap) {
		if (fileImage != null)
			fileImage.recycle();

		return fileImage = bitmap;
	}

	private static class FilePropertiesReceivedResponse implements ImmediateResponse<Map<String, String>, Notification> {

		private final boolean isPlaying;
		private final Bitmap bitmap;
		private final Context context;
		private final MediaSessionCompat mediaSessionCompat;
		private final PlaybackNotificationsConfiguration configuration;

		FilePropertiesReceivedResponse(boolean isPlaying, Bitmap bitmap, Context context, MediaSessionCompat mediaSessionCompat, PlaybackNotificationsConfiguration configuration) {
			this.isPlaying = isPlaying;
			this.bitmap = bitmap;
			this.context = context;
			this.mediaSessionCompat = mediaSessionCompat;
			this.configuration = configuration;
		}

		@Override
		public Notification respond(Map<String, String> fileProperties) {
			final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
			final String name = fileProperties.get(FilePropertiesProvider.NAME);

			final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, configuration.getNotificationChannel());
			builder
				.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
					.setCancelButtonIntent(PlaybackService.pendingKillService(context))
					.setMediaSession(mediaSessionCompat.getSessionToken())
					.setShowActionsInCompactView(1))
				.setOngoing(isPlaying)
				.setColor(ContextCompat.getColor(context, R.color.clearstream_dark))
				.setContentIntent(buildNowPlayingActivityIntent())
				.setDeleteIntent(PlaybackService.pendingKillService(context))
				.setShowWhen(false)
				.setSmallIcon(R.drawable.clearstream_logo_dark)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.setContentTitle(name)
				.setContentText(artist)
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
					PlaybackService.pendingNextIntent(context)));

			if (bitmap != null)
				builder.setLargeIcon(bitmap);

			return builder.build();
		}

		private PendingIntent buildNowPlayingActivityIntent() {
			// Set the notification area
			final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
			viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			return PendingIntent.getActivity(context, 0, viewIntent, 0);
		}
	}
}
