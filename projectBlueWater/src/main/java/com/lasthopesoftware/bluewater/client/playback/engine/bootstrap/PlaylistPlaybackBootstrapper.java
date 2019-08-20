package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap;

import com.lasthopesoftware.bluewater.client.playback.engine.ActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.playlist.playablefile.PlayableFilePlayer;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;

public final class PlaylistPlaybackBootstrapper implements StartAndClosePlayback {

	private final PlaylistVolumeManager volumeManagement;

	private PlayableFilePlayer playlistPlayer;
	private ActivePlayer activePlayer;

	public PlaylistPlaybackBootstrapper(PlaylistVolumeManager volumeManagement) {
		this.volumeManagement = volumeManagement;
	}

	@Override
	public IActivePlayer startPlayback(PreparedPlayableFileQueue preparedPlaybackQueue, final long filePosition) {
		close();

		playlistPlayer = new PlayableFilePlayer(preparedPlaybackQueue, filePosition);
		activePlayer = new ActivePlayer(playlistPlayer, volumeManagement);

		return activePlayer;
	}

	@Override
	public void close() {
		if (activePlayer != null) activePlayer.close();
		if (playlistPlayer != null)	playlistPlayer.close();
	}
}
