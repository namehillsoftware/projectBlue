package com.lasthopesoftware.bluewater.client.playback.file.volume;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;


public class PlaybackHandlerMaxVolumeModifier implements IVolumeManagement {

	private final IPlaybackHandler playbackHandler;
	private final float maxFileVolume;

	public PlaybackHandlerMaxVolumeModifier(IPlaybackHandler playbackHandler, float maxFileVolume) {
		this.playbackHandler = playbackHandler;
		this.maxFileVolume = maxFileVolume;
	}

	@Override
	public float setVolume(float volume) {
		playbackHandler.setVolume(maxFileVolume * volume);
		return playbackHandler.getVolume();
	}
}
