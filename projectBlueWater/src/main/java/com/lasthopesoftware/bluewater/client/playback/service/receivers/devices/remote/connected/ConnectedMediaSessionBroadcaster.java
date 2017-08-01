package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.RequiresApi;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static android.media.session.PlaybackState.ACTION_FAST_FORWARD;
import static android.media.session.PlaybackState.ACTION_PAUSE;
import static android.media.session.PlaybackState.ACTION_PLAY;
import static android.media.session.PlaybackState.ACTION_PLAY_FROM_MEDIA_ID;
import static android.media.session.PlaybackState.ACTION_PLAY_FROM_SEARCH;
import static android.media.session.PlaybackState.ACTION_PLAY_FROM_URI;
import static android.media.session.PlaybackState.ACTION_PLAY_PAUSE;
import static android.media.session.PlaybackState.ACTION_PREPARE;
import static android.media.session.PlaybackState.ACTION_PREPARE_FROM_MEDIA_ID;
import static android.media.session.PlaybackState.ACTION_PREPARE_FROM_SEARCH;
import static android.media.session.PlaybackState.ACTION_PREPARE_FROM_URI;
import static android.media.session.PlaybackState.ACTION_REWIND;
import static android.media.session.PlaybackState.ACTION_SEEK_TO;
import static android.media.session.PlaybackState.ACTION_SET_RATING;
import static android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT;
import static android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS;
import static android.media.session.PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM;
import static android.media.session.PlaybackState.ACTION_STOP;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ConnectedMediaSessionBroadcaster implements IConnectedDeviceBroadcaster {
	private static final Logger logger = LoggerFactory.getLogger(ConnectedMediaSessionBroadcaster.class);

	private static final float playbackSpeed = 1.0f;

	@Actions private static final long standardCapabilities =
		ACTION_PLAY_PAUSE |
		ACTION_SKIP_TO_NEXT |
		ACTION_SKIP_TO_PREVIOUS |
		ACTION_STOP;

	private final Context context;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private final ImageProvider imageProvider;
	private final MediaSession mediaSession;

	private volatile int playbackState = PlaybackState.STATE_STOPPED;
	private volatile int trackPosition = -1;
	private volatile MediaMetadata mediaMetadata = (new MediaMetadata.Builder()).build();
	@Actions private volatile long capabilities = standardCapabilities;
	private Bitmap remoteClientBitmap;

	public ConnectedMediaSessionBroadcaster(Context context, CachedFilePropertiesProvider cachedFilePropertiesProvider, ImageProvider imageProvider, MediaSession mediaSession) {
		this.context = context;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		this.imageProvider = imageProvider;
		this.mediaSession = mediaSession;
	}

	@Override
	public void setPlaying() {
		final PlaybackState.Builder builder = new PlaybackState.Builder();
		capabilities = ACTION_PAUSE | standardCapabilities;
		builder.setActions(capabilities);
		playbackState = PlaybackState.STATE_PLAYING;
		builder.setState(
			playbackState,
			trackPosition,
			playbackSpeed);
		mediaSession.setPlaybackState(builder.build());
	}

	@Override
	public void setPaused() {
		final PlaybackState.Builder builder = new PlaybackState.Builder();
		capabilities = ACTION_PLAY | standardCapabilities;
		builder.setActions(capabilities);
		playbackState = PlaybackState.STATE_PAUSED;
		builder.setState(
			playbackState,
			trackPosition,
			playbackSpeed);
		mediaSession.setPlaybackState(builder.build());
		updateClientBitmap(null);
	}

	@Override
	public void setStopped() {
		final PlaybackState.Builder builder = new PlaybackState.Builder();
		capabilities = PlaybackState.ACTION_PLAY | standardCapabilities;
		builder.setActions(capabilities);
		playbackState = PlaybackState.STATE_STOPPED;
		builder.setState(
			playbackState,
			trackPosition,
			playbackSpeed);
		mediaSession.setPlaybackState(builder.build());
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

				final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder(mediaMetadata);
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, artist);
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, album);
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, name);
				metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, duration);
				if (trackNumber != null)
					metadataBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, trackNumber.longValue());
				mediaSession.setMetadata(mediaMetadata = metadataBuilder.build());

				return null;
			}, context));
	}

	@Override
	public void updateTrackPosition(int trackPosition) {
		final PlaybackState.Builder builder = new PlaybackState.Builder();
		builder.setActions(capabilities);
		builder.setState(
			playbackState,
			this.trackPosition = trackPosition,
			playbackSpeed);
		mediaSession.setPlaybackState(builder.build());
	}

	private synchronized Void updateClientBitmap(Bitmap bitmap) {
		if (remoteClientBitmap == bitmap) return null;

		final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder(mediaMetadata);
		metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);
		mediaSession.setMetadata(mediaMetadata = metadataBuilder.build());

		// Track the remote client bitmap and recycle it in case the remote control client
		// does not properly recycle the bitmap
		if (remoteClientBitmap != null) remoteClientBitmap.recycle();
		remoteClientBitmap = bitmap;

		return null;
	}

	@IntDef(flag=true, value={ACTION_STOP, ACTION_PAUSE, ACTION_PLAY, ACTION_REWIND,
		ACTION_SKIP_TO_PREVIOUS, ACTION_SKIP_TO_NEXT, ACTION_FAST_FORWARD, ACTION_SET_RATING,
		ACTION_SEEK_TO, ACTION_PLAY_PAUSE, ACTION_PLAY_FROM_MEDIA_ID, ACTION_PLAY_FROM_SEARCH,
		ACTION_SKIP_TO_QUEUE_ITEM, ACTION_PLAY_FROM_URI, ACTION_PREPARE,
		ACTION_PREPARE_FROM_MEDIA_ID, ACTION_PREPARE_FROM_SEARCH, ACTION_PREPARE_FROM_URI})
	private @interface Actions {}
}
