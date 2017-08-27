package com.lasthopesoftware.bluewater.client.playback.state.bootstrap;

import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.playback.state.ActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.state.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;

import java.io.Closeable;
import java.io.IOException;

public final class PlaylistPlaybackBootstrapper implements IStartPlayback, Closeable {

	private final PlaylistVolumeManager volumeManagement;
	private final IPlaybackHandlerVolumeControllerFactory volumeControllerFactory;

	private PlaylistPlayer playlistPlayer;
	private ActivePlayer activePlayer;

	public PlaylistPlaybackBootstrapper(PlaylistVolumeManager volumeManagement, IPlaybackHandlerVolumeControllerFactory volumeControllerFactory) {
		this.volumeManagement = volumeManagement;
		this.volumeControllerFactory = volumeControllerFactory;
	}

	@Override
	public IActivePlayer startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final int filePosition) throws IOException {
		close();

		playlistPlayer = new PlaylistPlayer(preparedPlaybackQueue, volumeControllerFactory, filePosition);
		activePlayer = new ActivePlayer(playlistPlayer, volumeManagement);

		return activePlayer;
	}

	@Override
	public void close() throws IOException {
		if (activePlayer != null) activePlayer.close();
		if (playlistPlayer != null)	playlistPlayer.close();
	}
}
