package com.lasthopesoftware.bluewater.client.playback.state.volume;

import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.IPlaylistPlayer;


public class PlaylistVolumeManager implements IVolumeManagement {
	private IPlaylistPlayer playlistPlayer;
	private float volume;

	public PlaylistVolumeManager(float initialVolume) {
		volume = initialVolume;
	}

	public void managePlayer(IPlaylistPlayer playlistPlayer) {
		this.playlistPlayer = playlistPlayer;
		this.playlistPlayer.setVolume(volume);
	}

	@Override
	public float setVolume(float volume) {
		this.volume = volume;

		if (playlistPlayer != null)
			playlistPlayer.setVolume(this.volume);

		return this.volume;
	}
}
