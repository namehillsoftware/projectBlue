package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.volume;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;


public class SimpleExoPlayerVolumeManager implements ManagePlayableFileVolume {

	private final SimpleExoPlayer simpleExoPlayer;

	public SimpleExoPlayerVolumeManager(SimpleExoPlayer simpleExoPlayer) {
		this.simpleExoPlayer = simpleExoPlayer;
	}

	@Override
	public void setVolume(float volume) {
		simpleExoPlayer.setVolume(volume);
	}

	@Override
	public float getVolume() {
		return simpleExoPlayer.getVolume();
	}
}
