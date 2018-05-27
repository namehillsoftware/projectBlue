package com.lasthopesoftware.bluewater.client.playback.service.notification.building;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Map;

public class NowPlayingNotificationBuilder
implements
	BuildNowPlayingNotificationContent,
	AutoCloseable {

	private final IConnectionProvider connectionProvider;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private final ImageProvider imageProvider;
	private final Context context;
	private final SetupMediaStyleNotifications mediaStyleNotificationSetup;

	private ViewStructure viewStructure;

	public NowPlayingNotificationBuilder(Context context, SetupMediaStyleNotifications mediaStyleNotificationSetup, IConnectionProvider connectionProvider, CachedFilePropertiesProvider cachedFilePropertiesProvider, ImageProvider imageProvider) {
		this.context = context;
		this.mediaStyleNotificationSetup = mediaStyleNotificationSetup;
		this.connectionProvider = connectionProvider;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		this.imageProvider = imageProvider;
	}

	@Override
	public synchronized Promise<NotificationCompat.Builder> promiseNowPlayingNotification(ServiceFile serviceFile, boolean isPlaying) {
		final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile.getKey());

		if (viewStructure != null && !viewStructure.urlKeyHolder.equals(urlKeyHolder)) {
			viewStructure.release();
			viewStructure = null;
		}

		if (viewStructure == null)
			viewStructure = new ViewStructure(urlKeyHolder, serviceFile);

		if (viewStructure.promisedNowPlayingImage == null)
			viewStructure.promisedNowPlayingImage = imageProvider.promiseFileBitmap(serviceFile);

		if (viewStructure.promisedFileProperties == null)
			viewStructure.promisedFileProperties = cachedFilePropertiesProvider.promiseFileProperties(serviceFile);

		return viewStructure.promisedFileProperties
			.eventually(fileProperties -> {
				final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
				final String name = fileProperties.get(FilePropertiesProvider.NAME);

				final NotificationCompat.Builder builder = mediaStyleNotificationSetup.getMediaStyleNotification();
				builder
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
					.addAction(
						isPlaying
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

				if (!viewStructure.urlKeyHolder.equals(urlKeyHolder))
					return new Promise<>(builder);

				return viewStructure
					.promisedNowPlayingImage
					.then(
						bitmap -> {
							if (bitmap != null)
								builder.setLargeIcon(bitmap);

							return builder;
						},
						e -> builder);
			});
	}

	private PendingIntent buildNowPlayingActivityIntent() {
		// Set the notification area
		final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(context, 0, viewIntent, 0);
	}

	@Override
	public void close() {
		if (viewStructure != null)
			viewStructure.release();
	}

	private static class ViewStructure {
		final UrlKeyHolder<Integer> urlKeyHolder;
		final ServiceFile serviceFile;
		Promise<Map<String, String>> promisedFileProperties;
		Promise<Bitmap> promisedNowPlayingImage;

		ViewStructure(UrlKeyHolder<Integer> urlKeyHolder, ServiceFile serviceFile) {
			this.urlKeyHolder = urlKeyHolder;
			this.serviceFile = serviceFile;
		}

		void release() {
			if (promisedNowPlayingImage == null) return;

			promisedNowPlayingImage
				.then(bitmap -> {
					if (bitmap != null)
						bitmap.recycle();

					return null;
				});

			promisedNowPlayingImage.cancel();
		}
	}
}
