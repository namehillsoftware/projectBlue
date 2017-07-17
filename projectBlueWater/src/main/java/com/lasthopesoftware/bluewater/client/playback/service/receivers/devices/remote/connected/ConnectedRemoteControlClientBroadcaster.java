package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected;

import android.graphics.Bitmap;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectedRemoteControlClientBroadcaster implements IConnectedDeviceBroadcaster {
	private static final Logger logger = LoggerFactory.getLogger(ConnectedRemoteControlClientBroadcaster.class);

	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private final ImageProvider imageProvider;
	private final RemoteControlClient remoteControlClient;

	private volatile int playstate = RemoteControlClient.PLAYSTATE_STOPPED;
	private Bitmap remoteClientBitmap;

	public ConnectedRemoteControlClientBroadcaster(CachedFilePropertiesProvider cachedFilePropertiesProvider, ImageProvider imageProvider, RemoteControlClient remoteControlClient) {
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		this.imageProvider = imageProvider;
		this.remoteControlClient = remoteControlClient;
	}

	@Override
	public void setPlaying() {
		remoteControlClient.setPlaybackState(playstate = RemoteControlClient.PLAYSTATE_PLAYING);
	}

	@Override
	public void setPaused() {
		remoteControlClient.setPlaybackState(playstate = RemoteControlClient.PLAYSTATE_PAUSED);
		updateClientBitmap(null);
	}

	@Override
	public void setStopped() {
		remoteControlClient.setPlaybackState(playstate = RemoteControlClient.PLAYSTATE_STOPPED);
		updateClientBitmap(null);
	}

	@Override
	public void updateNowPlaying(ServiceFile serviceFile) {
		imageProvider
			.promiseFileBitmap(serviceFile)
			.next(this::updateClientBitmap)
			.error(e -> {
				logger.warn("There was an error getting the image for the file with id `" + serviceFile.getKey() + "`", e);
				return null;
			});

		cachedFilePropertiesProvider
			.promiseFileProperties(serviceFile.getKey())
			.next(fileProperties -> {
				final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
				final String name = fileProperties.get(FilePropertiesProvider.NAME);
				final String album = fileProperties.get(FilePropertiesProvider.ALBUM);
				final long duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);
				final String trackNumberString = fileProperties.get(FilePropertiesProvider.TRACK);
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
			});
	}

	@Override
	public void updateTrackPosition(int trackPosition) {
		remoteControlClient.setPlaybackState(playstate, trackPosition, 1.0f);
	}

	private Void updateClientBitmap(Bitmap bitmap) {
		if (remoteClientBitmap == bitmap) return null;

		final RemoteControlClient.MetadataEditor metaData = remoteControlClient.editMetadata(false);
		metaData.putBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, bitmap).apply();

		// Track the remote client bitmap and recycle it in case the remote control client
		// does not properly recycle the bitmap
		if (remoteClientBitmap != null) remoteClientBitmap.recycle();
		remoteClientBitmap = bitmap;

		return null;
	}
}
