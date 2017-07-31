package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectedMediaSessionBroadcaster implements IConnectedDeviceBroadcaster {
	private static final Logger logger = LoggerFactory.getLogger(ConnectedMediaSessionBroadcaster.class);

	private static final float playbackSpeed = 1.0f;

	private static final long standardCapabilities =
		PlaybackStateCompat.ACTION_PLAY_PAUSE |
		PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
		PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
		PlaybackStateCompat.ACTION_STOP;

	private final Context context;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private final ImageProvider imageProvider;
	private final MediaSessionCompat mediaSessionCompat;

	private volatile int playbackState = PlaybackStateCompat.STATE_STOPPED;
	private volatile int trackPosition = -1;
	private volatile MediaMetadataCompat mediaMetadataCompat = (new MediaMetadataCompat.Builder()).build();
	private volatile long capabilities = standardCapabilities;
	private Bitmap remoteClientBitmap;

	public ConnectedMediaSessionBroadcaster(Context context, CachedFilePropertiesProvider cachedFilePropertiesProvider, ImageProvider imageProvider, MediaSessionCompat mediaSessionCompat) {
		this.context = context;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		this.imageProvider = imageProvider;
		this.mediaSessionCompat = mediaSessionCompat;
	}

	@Override
	public void setPlaying() {
		final PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
		builder.setActions(capabilities = PlaybackStateCompat.ACTION_PAUSE | standardCapabilities);
		builder.setState(
			playbackState = PlaybackStateCompat.STATE_PLAYING,
			trackPosition,
			playbackSpeed);
		mediaSessionCompat.setPlaybackState(builder.build());
	}

	@Override
	public void setPaused() {
		final PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
		builder.setActions(capabilities = PlaybackStateCompat.ACTION_PLAY | standardCapabilities);
		builder.setState(
			playbackState = PlaybackStateCompat.STATE_PAUSED,
			trackPosition,
			playbackSpeed);
		mediaSessionCompat.setPlaybackState(builder.build());
		updateClientBitmap(null);
	}

	@Override
	public void setStopped() {
		final PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
		builder.setActions(capabilities = PlaybackStateCompat.ACTION_PLAY | standardCapabilities);
		builder.setState(
			playbackState = PlaybackStateCompat.STATE_STOPPED,
			trackPosition,
			playbackSpeed);
		mediaSessionCompat.setPlaybackState(builder.build());
		updateClientBitmap(null);
	}

	@Override
	public void updateNowPlaying(ServiceFile serviceFile) {
		imageProvider
			.promiseFileBitmap(serviceFile)
			.then(Dispatch.toContext(this::updateClientBitmap, context))
			.excuse(e -> {
				logger.warn("There was an error getting the image for the file with id `" + serviceFile.getKey() + "`", e);
				return null;
			});

		cachedFilePropertiesProvider
			.promiseFileProperties(serviceFile.getKey())
			.then(Dispatch.toContext(fileProperties -> {
				final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
				final String name = fileProperties.get(FilePropertiesProvider.NAME);
				final String album = fileProperties.get(FilePropertiesProvider.ALBUM);
				final long duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);
				final String trackNumberString = fileProperties.get(FilePropertiesProvider.TRACK);
				final Integer trackNumber = trackNumberString != null && !trackNumberString.isEmpty() ? Integer.valueOf(trackNumberString) : null;

				final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder(mediaMetadataCompat);
				metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);
				metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album);
				metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, name);
				metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
				if (trackNumber != null)
					metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber.longValue());
				mediaSessionCompat.setMetadata(mediaMetadataCompat = metadataBuilder.build());

				return null;
			}, context));
	}

	@Override
	public void updateTrackPosition(int trackPosition) {
		final PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
		builder.setActions(capabilities);
		builder.setState(
			playbackState,
			this.trackPosition = trackPosition,
			playbackSpeed);
		mediaSessionCompat.setPlaybackState(builder.build());
	}

	private synchronized Void updateClientBitmap(Bitmap bitmap) {
		if (remoteClientBitmap == bitmap) return null;

		final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder(mediaMetadataCompat);
		metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
		mediaSessionCompat.setMetadata(mediaMetadataCompat = metadataBuilder.build());

		// Track the remote client bitmap and recycle it in case the remote control client
		// does not properly recycle the bitmap
		if (remoteClientBitmap != null) remoteClientBitmap.recycle();
		remoteClientBitmap = bitmap;

		return null;
	}
}
