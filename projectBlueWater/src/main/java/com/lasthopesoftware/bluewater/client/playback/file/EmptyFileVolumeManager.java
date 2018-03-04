package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;


public class EmptyFileVolumeManager implements ManagePlayableFileVolume {
	private float volume;

	@Override
	public float setVolume(float volume) {
		return this.volume = volume;
	}

	@Override
	public float getVolume() {
		return this.volume;
	}
}
