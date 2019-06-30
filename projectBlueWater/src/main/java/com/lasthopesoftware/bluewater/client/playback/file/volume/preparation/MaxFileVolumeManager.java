package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;

public class MaxFileVolumeManager implements ManagePlayableFileVolume {
	private final ManagePlayableFileVolume playableFileVolume;
	private float unadjustedVolume = 1;
	private float maxFileVolume = 1;

	public MaxFileVolumeManager(ManagePlayableFileVolume playableFileVolume) {
		this.playableFileVolume = playableFileVolume;
	}

	@Override
	public float setVolume(float volume) {
		unadjustedVolume = volume;
		final float adjustedVolume = maxFileVolume * volume;
		playableFileVolume.setVolume(adjustedVolume);
		return adjustedVolume;
	}

	@Override
	public float getVolume() {
		return playableFileVolume.getVolume();
	}

	public void setMaxFileVolume(float maxFileVolume) {
		this.maxFileVolume = maxFileVolume;
		setVolume(unadjustedVolume);
	}
}
