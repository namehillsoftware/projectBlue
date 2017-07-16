package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected;

import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;

public class ConnectedRemoteControlClientBroadcaster implements IConnectedDeviceBroadcaster {

	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private final RemoteControlClient remoteControlClient;

	private volatile int playstate = RemoteControlClient.PLAYSTATE_STOPPED;

	public ConnectedRemoteControlClientBroadcaster(CachedFilePropertiesProvider cachedFilePropertiesProvider, RemoteControlClient remoteControlClient) {
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		this.remoteControlClient = remoteControlClient;
	}

	@Override
	public void setPlaying() {
		remoteControlClient.setPlaybackState(playstate = RemoteControlClient.PLAYSTATE_PLAYING);
	}

	@Override
	public void setPaused() {
		remoteControlClient.setPlaybackState(playstate = RemoteControlClient.PLAYSTATE_PAUSED);
	}

	@Override
	public void updateNowPlaying(ServiceFile serviceFile) {
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
}
