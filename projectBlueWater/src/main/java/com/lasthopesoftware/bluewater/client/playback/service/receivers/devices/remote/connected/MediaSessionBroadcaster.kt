package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.IRemoteBroadcaster;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MediaSessionBroadcaster implements IRemoteBroadcaster {
	private static final Logger logger = LoggerFactory.getLogger(MediaSessionBroadcaster.class);

	private static final float playbackSpeed = 1.0f;

	@Actions private static final long standardCapabilities =
		PlaybackState.ACTION_PLAY_PAUSE |
		PlaybackState.ACTION_SKIP_TO_NEXT |
		PlaybackState.ACTION_SKIP_TO_PREVIOUS |
		PlaybackState.ACTION_STOP;

	private final Context context;
	private final ScopedCachedFilePropertiesProvider scopedCachedFilePropertiesProvider;
	private final ImageProvider imageProvider;
	private final MediaSessionCompat mediaSession;

	private volatile int playbackState = PlaybackState.STATE_STOPPED;
	private volatile long trackPosition = -1;
	private volatile MediaMetadataCompat mediaMetadata = (new MediaMetadataCompat.Builder()).build();
	@Actions private volatile long capabilities = standardCapabilities;
	private Bitmap remoteClientBitmap;
	private volatile boolean isPlaying;

	public MediaSessionBroadcaster(Context context, ScopedCachedFilePropertiesProvider scopedCachedFilePropertiesProvider, ImageProvider imageProvider, MediaSessionCompat mediaSession) {
		this.context = context;
		this.scopedCachedFilePropertiesProvider = scopedCachedFilePropertiesProvider;
		this.imageProvider = imageProvider;
		this.mediaSession = mediaSession;
	}

	@Override
	public void setPlaying() {
		isPlaying = true;
		final PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
		capabilities = PlaybackState.ACTION_PAUSE | standardCapabilities;
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
		isPlaying = false;
		final PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
		capabilities = PlaybackState.ACTION_PLAY | standardCapabilities;
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
		isPlaying = false;
		final PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
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
		scopedCachedFilePropertiesProvider
			.promiseFileProperties(serviceFile)
			.eventually(LoopedInPromise.response(fileProperties -> {
				final String artist = fileProperties.get(KnownFileProperties.ARTIST);
				final String name = fileProperties.get(KnownFileProperties.NAME);
				final String album = fileProperties.get(KnownFileProperties.ALBUM);
				final long duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);
				final String trackNumberString = fileProperties.get(KnownFileProperties.TRACK);
				final Integer trackNumber = trackNumberString != null && !trackNumberString.isEmpty() ? Integer.valueOf(trackNumberString) : null;

				final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder(mediaMetadata);
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, artist);
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, album);
				metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, name);
				metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, duration);
				if (trackNumber != null) {
					metadataBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, trackNumber.longValue());
				}
				mediaSession.setMetadata(mediaMetadata = metadataBuilder.build());

				return null;
			}, context));

		if (!isPlaying) {
			updateClientBitmap(null);
			return;
		}

		imageProvider
			.promiseFileBitmap(serviceFile)
			.eventually(LoopedInPromise.response(this::updateClientBitmap, context))
			.excuse(e -> {
				logger.warn("There was an error getting the image for the file with id `" + serviceFile.getKey() + "`", e);
				return null;
			});
	}

	@Override
	public void updateTrackPosition(long trackPosition) {
		final PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
		builder.setActions(capabilities);
		builder.setState(
			playbackState,
			this.trackPosition = trackPosition,
			playbackSpeed);
		mediaSession.setPlaybackState(builder.build());
	}

	private synchronized Void updateClientBitmap(Bitmap bitmap) {
		if (remoteClientBitmap == bitmap) return null;

		final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder(mediaMetadata);
		metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);
		mediaSession.setMetadata(mediaMetadata = metadataBuilder.build());

		remoteClientBitmap = bitmap;

		return null;
	}

	@IntDef(flag=true, value={
		(int)PlaybackState.ACTION_STOP,
		(int)PlaybackState.ACTION_PAUSE,
		(int)PlaybackState.ACTION_PLAY,
		(int)PlaybackState.ACTION_REWIND,
		(int)PlaybackState.ACTION_SKIP_TO_PREVIOUS,
		(int)PlaybackState.ACTION_SKIP_TO_NEXT,
		(int)PlaybackState.ACTION_FAST_FORWARD,
		(int)PlaybackState.ACTION_SET_RATING,
		(int)PlaybackState.ACTION_SEEK_TO,
		(int)PlaybackState.ACTION_PLAY_PAUSE,
		(int)PlaybackState.ACTION_PLAY_FROM_MEDIA_ID,
		(int)PlaybackState.ACTION_PLAY_FROM_SEARCH,
		(int)PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM})
	private @interface Actions {}
}
