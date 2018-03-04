package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.volume;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;


public class SimpleExoPlayerVolumeManager implements ManagePlayableFileVolume {

	private final SimpleExoPlayer simpleExoPlayer;
	private float volume;

	public SimpleExoPlayerVolumeManager(SimpleExoPlayer simpleExoPlayer) {
		this.simpleExoPlayer = simpleExoPlayer;
	}

	@Override
	public float setVolume(float volume) {
		simpleExoPlayer.setVolume(this.volume = volume);
		return volume;
	}

	@Override
	public float getVolume() {
		return simpleExoPlayer.getVolume();
	}
}
