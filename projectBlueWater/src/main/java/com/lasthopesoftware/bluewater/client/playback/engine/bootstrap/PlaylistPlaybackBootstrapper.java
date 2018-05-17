package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap;

import com.lasthopesoftware.bluewater.client.playback.engine.ActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.playablefile.PlayableFilePlayer;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;

import java.io.Closeable;
import java.io.IOException;

public final class PlaylistPlaybackBootstrapper implements IStartPlayback, Closeable {

	private final PlaylistVolumeManager volumeManagement;
	private final IPlaybackHandlerVolumeControllerFactory volumeControllerFactory;

	private PlayableFilePlayer playlistPlayer;
	private ActivePlayer activePlayer;

	public PlaylistPlaybackBootstrapper(PlaylistVolumeManager volumeManagement, IPlaybackHandlerVolumeControllerFactory volumeControllerFactory) {
		this.volumeManagement = volumeManagement;
		this.volumeControllerFactory = volumeControllerFactory;
	}

	@Override
	public IActivePlayer startPlayback(PreparedPlayableFileQueue preparedPlaybackQueue, final long filePosition) throws IOException {
		close();

		playlistPlayer = new PlayableFilePlayer(preparedPlaybackQueue, volumeControllerFactory, filePosition);
		activePlayer = new ActivePlayer(playlistPlayer, volumeManagement);

		return activePlayer;
	}

	@Override
	public void close() {
		if (activePlayer != null) activePlayer.close();
		if (playlistPlayer != null)	playlistPlayer.close();
	}
}
