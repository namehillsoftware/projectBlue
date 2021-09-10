package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.IRemoteBroadcaster;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteControlClientBroadcaster implements IRemoteBroadcaster {
	private static final Logger logger = LoggerFactory.getLogger(RemoteControlClientBroadcaster.class);

	private static final float playbackSpeed = 1.0f;

	private static final int standardControlFlags =
			RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
			RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
			RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
			RemoteControlClient.FLAG_KEY_MEDIA_STOP;

	private final Context context;
	private final ScopedCachedFilePropertiesProvider scopedCachedFilePropertiesProvider;
	private final ImageProvider imageProvider;
	private final RemoteControlClient remoteControlClient;

	private volatile int playstate = RemoteControlClient.PLAYSTATE_STOPPED;
	private volatile long trackPosition = -1;
	private volatile boolean isPlaying;
	private Bitmap remoteClientBitmap;

	public RemoteControlClientBroadcaster(Context context, ScopedCachedFilePropertiesProvider scopedCachedFilePropertiesProvider, ImageProvider imageProvider, RemoteControlClient remoteControlClient) {
		this.context = context;
		this.scopedCachedFilePropertiesProvider = scopedCachedFilePropertiesProvider;
		this.imageProvider = imageProvider;
		this.remoteControlClient = remoteControlClient;
		remoteControlClient.setTransportControlFlags(standardControlFlags);
	}

	@Override
	public void setPlaying() {
		isPlaying = true;
		remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PAUSE | standardControlFlags);
		remoteControlClient.setPlaybackState(playstate = RemoteControlClient.PLAYSTATE_PLAYING, trackPosition, playbackSpeed);
	}

	@Override
	public void setPaused() {
		isPlaying = false;
		remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY | standardControlFlags);
		remoteControlClient.setPlaybackState(playstate = RemoteControlClient.PLAYSTATE_PAUSED, trackPosition, playbackSpeed);
		updateClientBitmap(null);
	}

	@Override
	public void setStopped() {
		isPlaying = false;
		remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY | standardControlFlags);
		remoteControlClient.setPlaybackState(playstate = RemoteControlClient.PLAYSTATE_STOPPED, trackPosition, playbackSpeed);
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

				final RemoteControlClient.MetadataEditor metaData = remoteControlClient.editMetadata(true);
				metaData.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, artist);
				metaData.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, album);
				metaData.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, name);
				metaData.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration);
				if (trackNumber != null)
					metaData.putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, trackNumber.longValue());
				metaData.apply();

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
		remoteControlClient.setPlaybackState(playstate, this.trackPosition = trackPosition, playbackSpeed);
	}

	private synchronized Void updateClientBitmap(Bitmap bitmap) {
		if (remoteClientBitmap == bitmap) return null;

		final RemoteControlClient.MetadataEditor metaData = remoteControlClient.editMetadata(false);
		metaData.putBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, bitmap).apply();

		remoteClientBitmap = bitmap;

		return null;
	}
}
