package com.lasthopesoftware.bluewater.client.playback.file.volume;

import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;


public class PlaybackHandlerMaxVolumeModifier implements IVolumeManagement {

	private final ManagePlayableFileVolume playableFileVolumeManager;
	private float unadjustedVolume;
	private float maxFileVolume = 1;

	public PlaybackHandlerMaxVolumeModifier(ManagePlayableFileVolume playableFileVolumeManager, float initialHandlerVolume) {
		this.playableFileVolumeManager = playableFileVolumeManager;
		this.unadjustedVolume = initialHandlerVolume;

		playableFileVolumeManager.setVolume(initialHandlerVolume);
	}

	public void setMaxFileVolume(float maxFileVolume) {
		this.maxFileVolume = maxFileVolume;
		setVolume(unadjustedVolume);
	}

	@Override
	public float setVolume(float volume) {
		unadjustedVolume = volume;
		final float adjustedVolume = maxFileVolume * volume;
		playableFileVolumeManager.setVolume(adjustedVolume);
		return adjustedVolume;
	}
}
