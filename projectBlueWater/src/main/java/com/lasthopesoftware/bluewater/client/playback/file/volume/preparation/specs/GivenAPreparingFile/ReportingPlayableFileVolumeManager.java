package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.GivenAPreparingFile;

import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;

class ReportingPlayableFileVolumeManager implements ManagePlayableFileVolume {

	public float volume;

	@Override
	public float setVolume(float volume) {
		return this.volume = volume;
	}

	@Override
	public float getVolume() {
		return volume;
	}
}
