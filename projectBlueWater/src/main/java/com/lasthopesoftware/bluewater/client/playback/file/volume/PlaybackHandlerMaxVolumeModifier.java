package com.lasthopesoftware.bluewater.client.playback.file.volume;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;


public class PlaybackHandlerMaxVolumeModifier implements IVolumeManagement {

	private final IPlaybackHandler playbackHandler;
	private float maxFileVolume = 1;

	public PlaybackHandlerMaxVolumeModifier(IPlaybackHandler playbackHandler) {
		this.playbackHandler = playbackHandler;
	}

	public void setMaxFileVolume(float maxFileVolume) {
		this.maxFileVolume = maxFileVolume;
	}

	@Override
	public float setVolume(float volume) {
		playbackHandler.setVolume(maxFileVolume * volume);
		return playbackHandler.getVolume();
	}
}
