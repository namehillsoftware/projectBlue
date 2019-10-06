package com.lasthopesoftware.bluewater.client.playback.service.notification.building;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.core.app.NotificationCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
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

				final NotificationCompat.Builder builder =
					addButtons(mediaStyleNotificationSetup.getMediaStyleNotification(), isPlaying)
						.setOngoing(isPlaying)
						.setContentTitle(name)
						.setContentText(artist);

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

	@Override
	public NotificationCompat.Builder getLoadingNotification(boolean isPlaying) {
		return addButtons(mediaStyleNotificationSetup.getMediaStyleNotification(), isPlaying)
			.setOngoing(isPlaying)
			.setContentTitle(context.getString(R.string.lbl_loading));
	}

	@Override
	public void close() {
		if (viewStructure != null)
			viewStructure.release();
	}

	private NotificationCompat.Builder addButtons(NotificationCompat.Builder builder, boolean isPlaying) {
		return builder
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
			if (promisedNowPlayingImage != null)
				promisedNowPlayingImage.cancel();
		}
	}
}
